package ru.gadjini.any2any.command.keyboard;

import com.antkorwin.xsync.XSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.archive.ArchiveState;
import ru.gadjini.any2any.service.keyboard.Any2AnyReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.QueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ArchiveCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveCommand.class);

    private static final long MAX_FILES_SIZE = 2100 * 1024 * 1024L;

    private ArchiveService archiveService;

    private QueueJob archiverJob;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private CommandStateService commandStateService;

    private Any2AnyReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private FormatService formatService;

    private Set<String> names = new HashSet<>();

    private MessageMediaService fileService;

    private XSync<Long> longXSync;

    @Autowired
    public ArchiveCommand(ArchiveService archiveService, QueueJob archiverJob, LocalisationService localisationService,
                          @Qualifier("messageLimits") MessageService messageService,
                          CommandStateService commandStateService, @Qualifier("curr") Any2AnyReplyKeyboardService replyKeyboardService,
                          UserService userService, FormatService formatService, MessageMediaService fileService, XSync<Long> longXSync) {
        this.archiveService = archiveService;
        this.archiverJob = archiverJob;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.commandStateService = commandStateService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.formatService = formatService;
        this.fileService = fileService;
        this.longXSync = longXSync;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.ARCHIVE_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        processMessage(message, (String) null);
    }

    @Override
    public String getCommandIdentifier() {
        return FileUtilsCommandNames.ARCHIVE_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                .text(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES, locale))
                .parseMode(ParseMode.HTML)
                .replyMarkup(replyKeyboardService.archiveTypesKeyboard(message.getChatId(), locale)).build());

        return true;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return FileUtilsCommandNames.ARCHIVE_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        longXSync.execute(message.getChatId(), () -> {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            if (message.hasText()) {
                ArchiveState archiveState = commandStateService.getState(message.getChatId(), getHistoryName(), false, ArchiveState.class);
                if (archiveState == null || archiveState.getFiles().isEmpty()) {
                    messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                            .text(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_EMPTY, locale))
                            .parseMode(ParseMode.HTML).build());
                } else {
                    Format associatedFormat = checkFormat(text, formatService.getAssociatedFormat(text), locale);
                    archiverJob.removeAndCancelCurrentTasks(message.getChatId());
                    archiveService.createArchive(message.getFrom().getId(), archiveState, associatedFormat);
                    commandStateService.deleteState(message.getChatId(), FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
                }
            } else {
                ArchiveState archiveState = commandStateService.getState(message.getChatId(), getHistoryName(), false, ArchiveState.class);
                if (archiveState == null) {
                    archiveState = new ArchiveState();
                }
                MessageMedia file = createFile(message, locale);
                validateTotalSize(archiveState.getFiles().stream().map(MessageMedia::getFileSize).mapToLong(i -> i).sum() + file.getFileSize(), locale);
                archiveState.getFiles().add(file);
                commandStateService.setState(message.getChatId(), getHistoryName(), archiveState);
            }
        });
    }

    @Override
    public void leave(long chatId) {
        archiverJob.removeAndCancelCurrentTasks(chatId);
    }

    private MessageMedia createFile(Message message, Locale locale) {
        MessageMedia any2AnyFile = fileService.getMedia(message, locale);
        if (any2AnyFile != null) {
            return any2AnyFile;
        } else {
            LOGGER.warn("No file message({}, {})", message.getFrom().getId(), TgMessage.getMetaTypes(message));
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_CANT_BE_ADDED_TO_ARCHIVE, locale));
        }
    }

    private Format checkFormat(String text, Format format, Locale locale) {
        if (format == null || format.getCategory() != FormatCategory.ARCHIVE) {
            LOGGER.warn("Incorrect archive format({})", text);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ARCHIVE_TYPE, locale));
        }

        return format;
    }

    private void validateTotalSize(long size, Locale locale) {
        if (size > MAX_FILES_SIZE) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_MAX_FILES_SIZE, new Object[]{
                    MemoryUtils.humanReadableByteCount(MAX_FILES_SIZE)
            }, locale));
        }
    }
}
