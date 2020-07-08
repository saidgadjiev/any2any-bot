package ru.gadjini.any2any.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.rename.RenameQueueService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Locale;

@Service
public class RenameService {

    private static final SmartLogger LOGGER = new SmartLogger(RenameService.class);

    private TelegramService telegramService;

    private TempFileService fileService;

    private FormatService formatService;

    private MessageService messageService;

    private RenameQueueService renameQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    @Autowired
    public RenameService(TelegramService telegramService, TempFileService fileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, RenameQueueService renameQueueService,
                         LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.renameQueueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
    }

    @PostConstruct
    public void init() {
        renameQueueService.resetProcessing();
    }

    @Autowired
    public void setExecutor(@Qualifier("renameTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectRenameTask(RenameTask renameTask) {
        renameQueueService.setWaiting(renameTask.jobId);
    }

    public RenameTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            RenameQueueItem peek = renameQueueService.pool(weight);

            if (peek != null) {
                return new RenameTask(peek.getId(), peek.getUserId(), peek.getFile().getFileName(), peek.getNewFileName(), peek.getFile().getMimeType(),
                        peek.getFile().getFileId(), peek.getFile().getSize(), peek.getReplyToMessageId());
            }
            return null;
        }
    }

    public void rename(int userId, RenameState renameState, String newFileName) {
        int jobId = renameQueueService.createProcessingItem(userId, renameState, newFileName);
        startRenaming(jobId, userId);
        executor.execute(new RenameTask(jobId, userId, renameState.getFile().getFileName(), newFileName,
                renameState.getFile().getMimeType(), renameState.getFile().getFileId(), renameState.getFile().getFileSize(), renameState.getReplyMessageId()));
    }

    public void cancelCurrentTasks(long chatId) {
        List<Integer> ids = renameQueueService.deleteByUserId((int) chatId);
        executor.cancelAndComplete(ids);

        RenameState state = commandStateService.getState(chatId, CommandNames.RENAME_COMMAND_NAME, false);
        if (state != null) {
            messageService.removeInlineKeyboard(chatId, state.getProcessingMessageId());
        }
    }

    public void cancel(int userId, int jobId) {
        renameQueueService.delete(jobId);
        executor.cancelAndComplete(jobId);
        RenameState state = commandStateService.getState(userId, CommandNames.RENAME_COMMAND_NAME, true);
        commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
        messageService.removeInlineKeyboard(userId, state.getProcessingMessageId());
    }

    public void leave(long chatId) {
        cancelCurrentTasks(chatId);
        commandStateService.deleteState(chatId, CommandNames.RENAME_COMMAND_NAME);
    }

    private void startRenaming(int jobId, int userId) {
        RenameState state = commandStateService.getState(userId, CommandNames.RENAME_COMMAND_NAME, true);
        Locale locale = new Locale(state.getLanguage());
        int messageId = messageService.sendMessage(
                new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING, locale))
                        .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale))).getMessageId();
        state.setProcessingMessageId(messageId);
        commandStateService.setState(userId, CommandNames.RENAME_COMMAND_NAME, state);
    }

    private SmartTempFile createNewFile(String fileName, String ext) {
        if (StringUtils.isNotBlank(ext)) {
            String withExt = FilenameUtils.getExtension(fileName);

            if (StringUtils.isBlank(withExt)) {
                return fileService.createTempFile(fileName + "." + ext);
            } else {
                return fileService.createTempFile(fileName);
            }
        }

        return fileService.createTempFile(fileName);
    }

    private void sendMessage(long chatId, int replyMessageId, File renamed) {
        try {
            messageService.sendDocument(new SendDocument(chatId, renamed).setReplyToMessageId(replyMessageId));
        } finally {
            FileUtils.deleteQuietly(renamed);
        }
    }

    public final class RenameTask implements SmartExecutorService.Job {

        private int jobId;
        private final int userId;
        private final String fileName;
        private final String newFileName;
        private final String mimeType;
        private final String fileId;
        private int fileSize;
        private final int replyToMessageId;

        private RenameTask(int jobId,
                           int userId,
                           String fileName,
                           String newFileName,
                           String mimeType,
                           String fileId,
                           int fileSize,
                           int replyToMessageId) {
            this.jobId = jobId;
            this.userId = userId;
            this.fileName = fileName;
            this.newFileName = newFileName;
            this.mimeType = mimeType;
            this.fileId = fileId;
            this.fileSize = fileSize;
            this.replyToMessageId = replyToMessageId;
        }

        @Override
        public void run() {
            LOGGER.debug("Start", userId, getWeight(), fileId);

            String ext = formatService.getExt(fileName, mimeType);
            SmartTempFile file = createNewFile(newFileName, ext);
            telegramService.downloadFileByFileId(fileId, file.getFile());
            RenameState renameState = commandStateService.getState(userId, CommandNames.RENAME_COMMAND_NAME, true);
            try {
                sendMessage(userId, replyToMessageId, file.getFile());

                LOGGER.debug("Finish", userId, getWeight(), newFileName);
            } catch (Exception ex) {
                messageService.sendErrorMessage(userId, new Locale(renameState.getLanguage()));
                throw ex;
            } finally {
                renameQueueService.delete(jobId);
                commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
                executor.complete(jobId);
                file.smartDelete();
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }
    }
}
