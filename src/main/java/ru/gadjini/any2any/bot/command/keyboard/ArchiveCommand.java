package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.*;

@Component
public class ArchiveCommand implements KeyboardBotCommand, NavigableBotCommand {

    private ArchiveService archiveService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private CommandStateService commandStateService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private FormatService formatService;

    private Set<String> names = new HashSet<>();

    @Autowired
    public ArchiveCommand(ArchiveService archiveService, LocalisationService localisationService, @Qualifier("limits") MessageService messageService,
                          CommandStateService commandStateService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                          UserService userService, FormatService formatService) {
        this.archiveService = archiveService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.formatService = formatService;
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
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES, locale))
                .replyKeyboard(replyKeyboardService.archiveTypesKeyboard(message.getChatId(), locale)));

        return true;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.ARCHIVE_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (message.hasText()) {
            List<Any2AnyFile> files = commandStateService.getState(message.getChatId(), getHistoryName(), false);
            if (files == null || files.isEmpty()) {
                messageService.sendMessage(new SendMessageContext(message.getChatId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_EMPTY, locale)));
            } else {
                Format associatedFormat = checkFormat(formatService.getAssociatedFormat(message.getText()), locale);
                archiveService.createArchive(message.getFrom().getId(), files, associatedFormat, locale);
                messageService.sendMessage(new SendMessageContext(message.getChatId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_PROCESSING, locale)));
                commandStateService.deleteState(message.getChatId(), getHistoryName());
            }
        } else {
            List<Any2AnyFile> files = commandStateService.getState(message.getChatId(), getHistoryName(), false);
            if (files == null) {
                files = new ArrayList<>();
            }
            files.add(createFile(message, locale));
            commandStateService.setState(message.getChatId(), getHistoryName(), files);
        }
    }

    private Any2AnyFile createFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = new Any2AnyFile();
        if (message.hasDocument()) {
            any2AnyFile.setFileName(message.getDocument().getFileName());
            any2AnyFile.setFileId(message.getDocument().getFileId());
        } else if (message.hasPhoto()) {
            any2AnyFile.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + ".jpg");
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            any2AnyFile.setFileId(photoSize.getFileId());
        } else {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_CANT_BE_ADDED_TO_ARCHIVE, locale));
        }

        return any2AnyFile;
    }

    private Format checkFormat(Format format, Locale locale) {
        if (format == null || format.getCategory() != FormatCategory.ARCHIVE) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ARCHIVE_TYPE, locale));
        }

        return format;
    }
}
