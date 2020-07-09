package ru.gadjini.any2any.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
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
import java.util.function.Supplier;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private TelegramService telegramService;

    private TempFileService fileService;

    private FormatService formatService;

    private MessageService messageService;

    private RenameQueueService renameQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    @Autowired
    public RenameService(TelegramService telegramService, TempFileService fileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, RenameQueueService renameQueueService,
                         LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.renameQueueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        renameQueueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("renameTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectRenameTask(SmartExecutorService.Job job) {
        renameQueueService.setWaiting(job.getId());
    }

    public RenameTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            RenameQueueItem peek = renameQueueService.poll(weight);

            if (peek != null) {
                return new RenameTask(peek);
            }
            return null;
        }
    }

    public void rename(int userId, RenameState renameState, String newFileName) {
        RenameQueueItem item = renameQueueService.createProcessingItem(userId, renameState, newFileName);
        startRenaming(item.getId(), userId);
        executor.execute(new RenameTask(item));
    }

    public void cancelCurrentTasks(long chatId) {
        List<Integer> ids = renameQueueService.deleteByUserId((int) chatId);
        executor.cancelAndComplete(ids, false);

        RenameState state = commandStateService.getState(chatId, CommandNames.RENAME_COMMAND_NAME, false);
        if (state != null) {
            messageService.removeInlineKeyboard(chatId, state.getProcessingMessageId());
        }
    }

    public void cancel(int userId, int jobId) {
        renameQueueService.delete(jobId);
        executor.cancelAndComplete(jobId, true);
        RenameState state = commandStateService.getState(userId, CommandNames.RENAME_COMMAND_NAME, true);
        commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
        messageService.removeInlineKeyboard(userId, state.getProcessingMessageId());
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void leave(long chatId) {
        cancelCurrentTasks(chatId);
        commandStateService.deleteState(chatId, CommandNames.RENAME_COMMAND_NAME);
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<RenameQueueItem> tasks = renameQueueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (RenameQueueItem item : tasks) {
            executor.execute(new RenameTask(item));
        }
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
        private volatile Supplier<Boolean> checker;
        private volatile boolean autoCancel;
        private volatile SmartTempFile file;

        private RenameTask(RenameQueueItem queueItem) {
            this.jobId = queueItem.getId();
            this.userId = queueItem.getUserId();
            this.fileName = queueItem.getFile().getFileName();
            this.newFileName = queueItem.getNewFileName();
            this.mimeType = queueItem.getFile().getMimeType();
            this.fileId = queueItem.getFile().getFileId();
            this.fileSize = queueItem.getFile().getSize();
            this.replyToMessageId = queueItem.getReplyToMessageId();
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {})", userId, size, fileId);

            try {
                String ext = formatService.getExt(fileName, mimeType);
                file = createNewFile(newFileName, ext);
                telegramService.downloadFileByFileId(fileId, file);
                sendMessage(userId, replyToMessageId, file.getFile());

                LOGGER.debug("Finish({}, {}, {})", userId, size, newFileName);
            } catch (Exception ex) {
                if (checker.get()) {
                    LOGGER.debug("Canceled({}, {})", userId, size);
                } else {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                cleanup();
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void cancel() {
            telegramService.cancelDownloading(fileId);
            cleanup();
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.autoCancel = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void cleanup() {
            if (!autoCancel) {
                renameQueueService.delete(jobId);
                commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
            }
            executor.complete(jobId);
            if (file != null) {
                file.smartDelete();
            }
        }
    }
}
