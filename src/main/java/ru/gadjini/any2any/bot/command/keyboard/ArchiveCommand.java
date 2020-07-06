package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.archive.ArchiveState;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.*;

@Component
public class ArchiveCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private ArchiveService archiveService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private CommandStateService commandStateService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private FormatService formatService;

    private Set<String> names = new HashSet<>();

    private FileService fileService;

    @Autowired
    public ArchiveCommand(ArchiveService archiveService, LocalisationService localisationService, @Qualifier("limits") MessageService messageService,
                          CommandStateService commandStateService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                          UserService userService, FormatService formatService, FileService fileService) {
        this.archiveService = archiveService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.formatService = formatService;
        this.fileService = fileService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.ARCHIVE_COMMAND_NAME, locale));
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
    public void processMessage(Message message) {
        processMessage(message, null);
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.ARCHIVE_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFromUser().getId());

        return true;
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.ARCHIVE_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        if (message.hasText()) {
            ArchiveState archiveState = commandStateService.getState(message.getChatId(), getHistoryName(), false);
            if (archiveState.getFiles().isEmpty()) {
                messageService.sendMessage(new HtmlMessage(message.getChatId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_EMPTY, locale)));
            } else {
                Format associatedFormat = checkFormat(formatService.getAssociatedFormat(message.getText()), locale);
                archiveService.createArchive(message.getFromUser().getId(), archiveState, associatedFormat);
            }
        } else {
            ArchiveState archiveState = commandStateService.getState(message.getChatId(), getHistoryName(), false);
            if (archiveState == null) {
                archiveState = new ArchiveState();
            }
            archiveState.getFiles().add(createFile(message, locale));
            commandStateService.setState(message.getChatId(), getHistoryName(), archiveState);
            messageService.sendMessage(
                    new HtmlMessage(
                            message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_CURRENT_FILES,
                            new Object[]{toString(archiveState.getFiles())}, locale)
                    )
            );
        }
    }

    @Override
    public void leave(long chatId) {
        archiveService.leave(chatId);
    }

    private Any2AnyFile createFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = fileService.getFile(message, locale);
        if (any2AnyFile != null) {
            return any2AnyFile;
        } else {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_CANT_BE_ADDED_TO_ARCHIVE, locale));
        }
    }

    private String toString(List<Any2AnyFile> any2AnyFiles) {
        StringBuilder files = new StringBuilder();
        for (Any2AnyFile any2AnyFile : any2AnyFiles) {
            if (files.length() > 0) {
                files.append(", ");
            }

            files.append(any2AnyFile.getFileName());
        }

        return files.toString();
    }

    private Format checkFormat(Format format, Locale locale) {
        if (format == null || format.getCategory() != FormatCategory.ARCHIVE) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ARCHIVE_TYPE, locale));
        }

        return format;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES, locale))
                .setReplyMarkup(replyKeyboardService.archiveTypesKeyboard(chatId, locale)));
    }
}
