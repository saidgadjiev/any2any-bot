package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.thumb.ThumbService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.thumb.ThumbState;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ThumbCommand implements KeyboardBotCommand, NavigableBotCommand {

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private final LocalisationService localisationService;

    private ThumbService thumbService;

    private MessageService messageService;

    private UserService userService;

    private ReplyKeyboardService replyKeyboardService;

    private FileService fileService;

    @Autowired
    public ThumbCommand(CommandStateService commandStateService, LocalisationService localisationService, ThumbService thumbService,
                        @Qualifier("limits") MessageService messageService, UserService userService,
                        @Qualifier("curr") ReplyKeyboardService replyKeyboardService, FileService fileService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.thumbService = thumbService;
        this.messageService = messageService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.fileService = fileService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.THUMB_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_WELCOME, locale))
                .setReplyMarkup(replyKeyboardService.goBack(message.getChatId(), locale)));

        return true;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        ThumbState thumbState = commandStateService.getState(message.getChatId(), CommandNames.THUMB_COMMAND, false);
        if (thumbState == null) {
            thumbState = new ThumbState();
            thumbState.setFile(fileService.getFile(message, locale));
            thumbState.setReplyToMessageId(message.getMessageId());
            commandStateService.setState(message.getChatId(), CommandNames.THUMB_COMMAND, thumbState);
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_SEND_THUMB, locale)));
        } else if (thumbState.getThumb() == null) {
            thumbState.setThumb(fileService.getFile(message, locale));
            thumbService.setThumb(message.getFromUser().getId(), thumbState);
        } else {
            thumbService.removeAndCancelCurrentTasks(message.getChatId());
        }
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.THUMB_COMMAND;
    }

    @Override
    public void leave(long chatId) {
        thumbService.leave(chatId);
    }
}
