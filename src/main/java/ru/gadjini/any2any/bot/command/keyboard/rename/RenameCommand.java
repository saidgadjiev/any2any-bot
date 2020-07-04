package ru.gadjini.any2any.bot.command.keyboard.rename;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class RenameCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameCommand.class);

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final RenameService renameService;

    private FileService fileService;

    @Autowired
    public RenameCommand(LocalisationService localisationService, CommandStateService commandStateService,
                         @Qualifier("limits") MessageService messageService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                         UserService userService, RenameService renameService, FileService fileService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.renameService = renameService;
        this.fileService = fileService;
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
    public void processMessage(Message message) {
        processMessage(message, null);
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.RENAME_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFromUser().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale))
                .setReplyMarkup(replyKeyboardService.goBack(chatId, locale)));
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.RENAME_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());

        if (!commandStateService.hasState(message.getChatId(), getHistoryName())) {
            RenameState renameState = createState(message, locale);
            renameState.setLanguage(locale.getLanguage());
            messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale)));
            commandStateService.setState(message.getChatId(), getHistoryName(), renameState);
        } else if (message.hasText()) {
            RenameState renameState = commandStateService.getState(message.getChatId(), getHistoryName(), true);
            renameService.rename(message.getFromUser().getId(), renameState, text, locale);
            commandStateService.deleteState(message.getChatId(), getHistoryName());
            messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING, locale)));
            LOGGER.debug("Rename request " + renameState.getFile().getFileId() + " with fileName " + renameState.getFile().getFileName() + " to " + text);
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }

    private RenameState createState(Message message, Locale locale) {
        RenameState renameState = new RenameState();
        renameState.setReplyMessageId(message.getMessageId());
        renameState.setFile(fileService.getFile(message, locale));

        if (renameState.getFile() == null) {
            LOGGER.debug("Not file message " + TgMessage.from(message));
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FORBIDDEN, locale));
        }

        return renameState;
    }
}
