package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.cleaner.GarbageFileCollector;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.file.FileManager;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MediaMessageService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class AdminCommand implements KeyboardBotCommand, NavigableBotCommand {

    private CommandStateService commandStateService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private GarbageFileCollector garbageFileCollector;

    private FileManager fileManager;

    private Set<String> names = new HashSet<>();

    @Autowired
    public AdminCommand(LocalisationService localisationService, @Qualifier("medialimits") MediaMessageService mediaMessageService,
                        GarbageFileCollector garbageFileCollector, FileManager fileManager) {
        this.localisationService = localisationService;
        this.mediaMessageService = mediaMessageService;
        this.garbageFileCollector = garbageFileCollector;
        this.fileManager = fileManager;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.ADMIN_COMMAND_NAME, locale));
        }
    }

    @Autowired
    public void setReplyKeyboardService(@Qualifier("curr") ReplyKeyboardService replyKeyboardService) {
        this.replyKeyboardService = replyKeyboardService;
    }

    @Autowired
    public void setMessageService(@Qualifier("messagelimits") MessageService messageService) {
        this.messageService = messageService;
    }

    @Autowired
    public void setCommandStateService(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        if (!userService.isAdmin((int) chatId)) {
            return false;
        }
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.ADMIN_COMMAND_NAME, locale))
                        .setReplyMarkup(replyKeyboardService.getAdminKeyboard(message.getChatId(), locale))
        );

        return true;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.ADMIN;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (text.equals(localisationService.getMessage(MessagesProperties.DOWNLOAD_FILE_COMMAND_NAME, locale))) {
            String fileId = commandStateService.getState(message.getChatId(), CommandNames.ADMIN, false, String.class);
            if (StringUtils.isNotBlank(fileId)) {
                commandStateService.deleteState(message.getChatId(), CommandNames.ADMIN);
                messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOAD_FILE_PROCESSING, locale)));
                mediaMessageService.sendFile(message.getChatId(), fileId);
            }
        } else if (text.equals(localisationService.getMessage(MessagesProperties.REMOVE_GARBAGE_FILES_COMMAND_NAME, locale))) {
            int deleted = garbageFileCollector.clean();
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_COLLECTED_GARBAGE, new Object[]{deleted}, locale)));
        } else if (text.equals(localisationService.getMessage(MessagesProperties.RESET_FILE_LIMIT_COMMAND_NAME, locale))) {
            fileManager.resetLimits(message.getChatId());
        } else {
            commandStateService.setState(message.getChatId(), CommandNames.ADMIN, text);
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, CommandNames.ADMIN);
    }
}
