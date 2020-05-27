package ru.gadjini.any2any.bot.command.keyboard.rename;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class RenameCommand implements KeyboardBotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameCommand.class);

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final RenameService renameService;

    @Autowired
    public RenameCommand(LocalisationService localisationService, CommandStateService commandStateService,
                         @Qualifier("limits") MessageService messageService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                         UserService userService, RenameService renameService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.renameService = renameService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.RENAME_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale))
                .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale)));

        return true;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.RENAME_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        if (!commandStateService.hasState(message.getChatId(), getHistoryName())) {
            checkMedia(message, locale);
            RenameState renameState = createState(message);
            renameState.setLanguage(locale.getLanguage());
            messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale)));
            commandStateService.setState(message.getChatId(), getHistoryName(), renameState);
        } else if (message.hasText()) {
            RenameState renameState = commandStateService.getState(message.getChatId(), getHistoryName(), true);
            renameService.rename(message.getChatId(), renameState, text);
            commandStateService.deleteState(message.getChatId(), getHistoryName());
            messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING, locale)));
            LOGGER.debug("Rename request " + renameState.getFileId() + " with fileName " + renameState.getFileName() + " to " + text);
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }

    private void checkMedia(Message message, Locale locale) {
        if (!message.hasDocument()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FORBIDDEN, locale));
        }
    }

    private RenameState createState(Message message) {
        RenameState renameState = new RenameState();
        renameState.setReplyMessageId(message.getMessageId());

        if (message.hasDocument()) {
            renameState.setFileId(message.getDocument().getFileId());
            renameState.setFileName(message.getDocument().getFileName());
            renameState.setMimeType(message.getDocument().getMimeType());
        }

        return renameState;
    }
}
