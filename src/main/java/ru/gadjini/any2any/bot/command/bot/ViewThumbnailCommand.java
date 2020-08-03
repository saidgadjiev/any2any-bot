package ru.gadjini.any2any.bot.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.HasThumb;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendPhoto;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.thumb.ThumbService;

import java.util.Locale;

@Component
public class ViewThumbnailCommand implements BotCommand {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ThumbService thumbService;

    private CommandNavigator commandNavigator;

    private CommandStateService commandStateService;

    private ThreadPoolTaskExecutor executor;

    @Autowired
    public ViewThumbnailCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                                UserService userService, ThumbService thumbService, CommandStateService commandStateService,
                                @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.commandStateService = commandStateService;
        this.executor = executor;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public void processMessage(Message message) {
        String currentCommandName = getCurrentCommandName(message.getChatId());
        if (StringUtils.isNotBlank(currentCommandName)) {
            HasThumb state = getState(message.getChatId(), currentCommandName);

            if (state != null) {
                Any2AnyFile thumbnail = state.getThumb();
                if (thumbnail != null) {
                    if (StringUtils.isNotBlank(thumbnail.getCachedFileId())) {
                        messageService.sendPhotoAsync(new SendPhoto(message.getChatId(), thumbnail.getCachedFileId()));
                    } else {
                        executor.execute(() -> {
                            SmartTempFile tempFile = thumbService.convertToThumb(message.getChatId(), thumbnail.getFileId(), thumbnail.getFileName(), thumbnail.getMimeType());
                            try {
                                messageService.sendPhotoAsync(new SendPhoto(message.getChatId(), tempFile.getAbsolutePath()), sendFileResult -> {
                                    thumbnail.setCachedFileId(sendFileResult.getFileId());
                                    commandStateService.setState(message.getChatId(), currentCommandName, state);
                                });
                            } finally {
                                tempFile.smartDelete();
                            }
                        });
                    }
                } else {
                    thumbNotFound(message);
                }
            } else {
                thumbNotFound(message);
            }
        } else {
            thumbNotFound(message);
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.VIEW_THUMBNAIL_COMMAND;
    }

    private void thumbNotFound(Message message) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        messageService.sendMessageAsync(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
    }

    private String getCurrentCommandName(long chatId) {
        NavigableBotCommand currentCommand = commandNavigator.getCurrentCommand(chatId);

        if (currentCommand != null) {
            return currentCommand.getHistoryName();
        }

        return null;
    }

    private HasThumb getState(long chatId, String commandName) {
        if (commandName.equals(CommandNames.RENAME_COMMAND_NAME)) {
            return commandStateService.getState(chatId, commandName, false, RenameState.class);
        }

        return null;
    }
}
