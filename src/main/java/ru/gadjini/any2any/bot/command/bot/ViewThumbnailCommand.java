package ru.gadjini.any2any.bot.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
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

    @Autowired
    public ViewThumbnailCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                                UserService userService, ThumbService thumbService, CommandStateService commandStateService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public void processMessage(Message message) {
        Any2AnyFile thumbnail = getThumb(message.getChatId());
        if (thumbnail != null) {
            SmartTempFile tempFile = thumbService.convertToThumb(thumbnail.getFileId(), thumbnail.getFileName(), thumbnail.getMimeType());
            try {
                messageService.sendPhoto(new SendPhoto(message.getChatId(), tempFile.getAbsolutePath()));
            } finally {
                tempFile.smartDelete();
            }
        } else {
            Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.VIEW_THUMBNAIL_COMMAND;
    }

    private Any2AnyFile getThumb(long chatId) {
        NavigableBotCommand currentCommand = commandNavigator.getCurrentCommand(chatId);

        if (currentCommand != null) {
            String commandName = currentCommand.getHistoryName();
            Object state = commandStateService.getState(chatId, commandName, false);

            if (state instanceof HasThumb) {
                return ((HasThumb) state).getThumb();
            }
        }

        return null;
    }
}
