package ru.gadjini.any2any.service.rename;

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
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.Progress;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.queue.rename.RenameQueueService;
import ru.gadjini.any2any.service.thumb.ThumbService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private TelegramService telegramService;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MessageService messageService;

    private RenameQueueService renameQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private ThumbService thumbService;

    private RenameMessageBuilder renameMessageBuilder;

    @Autowired
    public RenameService(TelegramService telegramService, TempFileService tempFileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, RenameQueueService renameQueueService,
                         LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService, ThumbService thumbService,
                         RenameMessageBuilder renameMessageBuilder) {
        this.telegramService = telegramService;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.renameQueueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.renameMessageBuilder = renameMessageBuilder;
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
        LOGGER.debug("Rejected({})", job.getWeight());
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
        int messageId = sendStartRenamingMessage(item.getId(), userId);
        item.setProgressMessageId(messageId);
        renameQueueService.setProgressMessageId(item.getId(), messageId);

        executor.execute(new RenameTask(item));
    }

    public void removeAndCancelCurrentTasks(long chatId) {
        RenameState renameState = commandStateService.getState(chatId, CommandNames.RENAME_COMMAND_NAME, false, RenameState.class);
        if (renameState != null && renameState.getFile() != null) {
            List<Integer> ids = renameQueueService.deleteByUserId((int) chatId);
            executor.cancelAndComplete(ids, false);
        }
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!renameQueueService.exists(jobId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, userService.getLocaleOrDefault((int) chatId)),
                    true
            ));
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))
            ));
            if (!executor.cancelAndComplete(jobId, true)) {
                renameQueueService.delete(jobId);
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void leave(long chatId) {
        List<Integer> ids = renameQueueService.deleteByUserId((int) chatId);
        if (ids.size() > 0) {
            LOGGER.debug("Leave({}, {})", chatId, ids.size());
        }
        executor.cancelAndComplete(ids, false);
        commandStateService.deleteState(chatId, CommandNames.RENAME_COMMAND_NAME);
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<RenameQueueItem> tasks = renameQueueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (RenameQueueItem item : tasks) {
            executor.execute(new RenameTask(item));
        }
    }

    private int sendStartRenamingMessage(int jobId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        return messageService.sendMessage(
                new HtmlMessage((long) userId, String.format(renameMessageBuilder.buildRenamingMessage(RenameStep.DOWNLOADING,
                        locale, Lang.JAVA), 0, localisationService.getMessage(MessagesProperties.MESSAGE_ETA_CALCULATED, locale)))
                        .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale))).getMessageId();
    }

    private String createNewFileName(String fileName, String ext) {
        if (StringUtils.isNotBlank(ext)) {
            String withExt = FilenameUtils.getExtension(fileName);

            if (StringUtils.isBlank(withExt)) {
                return fileName + "." + ext;
            }
        }

        return fileName;
    }

    private Progress progress(long chatId, int jobId, int processMessageId, RenameStep renameStep, RenameStep nextStep) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        Progress progress = new Progress();
        progress.setChatId(chatId);
        progress.setProgressMessageId(processMessageId);
        progress.setProgressMessage(renameMessageBuilder.buildRenamingMessage(renameStep, locale, Lang.PYTHON));
        if (nextStep != null) {
            String etaCalculated = localisationService.getMessage(MessagesProperties.MESSAGE_ETA_CALCULATED, locale);
            String completionMessage = renameMessageBuilder.buildRenamingMessage(nextStep, locale, Lang.JAVA);
            progress.setAfterProgressCompletionMessage(String.format(completionMessage, nextStep == RenameStep.RENAMING ? 50 : 0, nextStep == RenameStep.RENAMING
                            ? "7 seconds" : etaCalculated));
        }
        progress.setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale));

        return progress;
    }

    public final class RenameTask implements SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(RenameTask.class);

        public static final String TAG = "rename";

        private int jobId;
        private final int userId;
        private final String fileName;
        private final String newFileName;
        private final String mimeType;
        private final String fileId;
        private long fileSize;
        private final int replyToMessageId;
        private final int progressMessageId;
        private volatile Supplier<Boolean> checker;
        private volatile boolean canceledByUser;
        private volatile SmartTempFile file;
        private volatile SmartTempFile thumbFile;
        private TgFile userThumb;
        private String thumb;

        private RenameTask(RenameQueueItem queueItem) {
            this.jobId = queueItem.getId();
            this.userId = queueItem.getUserId();
            this.fileName = queueItem.getFile().getFileName();
            this.newFileName = queueItem.getNewFileName();
            this.mimeType = queueItem.getFile().getMimeType();
            this.fileId = queueItem.getFile().getFileId();
            this.fileSize = queueItem.getFile().getSize();
            this.replyToMessageId = queueItem.getReplyToMessageId();
            this.thumb = queueItem.getFile().getThumb();
            this.userThumb = queueItem.getThumb();
            this.progressMessageId = queueItem.getProgressMessageId();
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {})", userId, size, fileId);

            try {
                String ext = formatService.getExt(fileName, mimeType);
                file = tempFileService.createTempFile(userId, fileId, TAG, ext);
                telegramService.downloadFileByFileId(fileId, fileSize, progress(userId, jobId, progressMessageId, RenameStep.DOWNLOADING, RenameStep.RENAMING), file);

                String fileName = createNewFileName(newFileName, ext);
                if (userThumb != null) {
                    thumbFile = thumbService.convertToThumb(userId, userThumb.getFileId(), userThumb.getFileName(), userThumb.getMimeType());
                } else if (StringUtils.isNotBlank(thumb)) {
                    thumbFile = tempFileService.createTempFile(userId, fileId, TAG, Format.JPG.getExt());
                    telegramService.downloadFileByFileId(thumb, thumbFile);
                }
                messageService.sendDocument(new SendDocument((long) userId, fileName, file.getFile())
                        .setProgress(progress(userId, jobId, progressMessageId, RenameStep.UPLOADING, RenameStep.COMPLETED))
                        .setThumb(thumbFile != null ? thumbFile.getAbsolutePath() : null)
                        .setReplyToMessageId(replyToMessageId));

                LOGGER.debug("Finish({}, {}, {})", userId, size, newFileName);
            } catch (Exception ex) {
                if (!checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                if (!checker.get()) {
                    executor.complete(jobId);
                    renameQueueService.delete(jobId);
                    if (file != null) {
                        file.smartDelete();
                    }
                    if (thumbFile != null) {
                        thumbFile.smartDelete();
                    }
                }
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void cancel() {
            if (!telegramService.cancelDownloading(fileId) && file != null) {
                file.smartDelete();
            }
            if (!telegramService.cancelDownloading(thumb) && thumbFile != null) {
                thumbFile.smartDelete();
            }
            if (canceledByUser) {
                renameQueueService.delete(jobId);
                LOGGER.debug("Canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }
    }
}
