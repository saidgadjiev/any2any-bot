package ru.gadjini.any2any.service;

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
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.file.FileWorkObject;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.file.FileManager;
import ru.gadjini.any2any.service.message.MediaMessageService;
import ru.gadjini.any2any.service.message.MessageService;
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

    private FileManager fileManager;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private RenameQueueService renameQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private ThumbService thumbService;

    @Autowired
    public RenameService(FileManager fileManager, TempFileService tempFileService, FormatService formatService,
                         @Qualifier("messagelimits") MessageService messageService,
                         @Qualifier("medialimits") MediaMessageService mediaMessageService, RenameQueueService renameQueueService,
                         LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService, ThumbService thumbService) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.renameQueueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.thumbService = thumbService;
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
        sendStartRenamingMessage(item.getId(), userId);
        fileManager.setInputFilePending(userId, renameState.getReplyMessageId());
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

    private void sendStartRenamingMessage(int jobId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(
                new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING, locale))
                        .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale)));
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

    public final class RenameTask implements SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(RenameTask.class);

        public static final String TAG = "rename";

        private int jobId;
        private final int userId;
        private final String fileName;
        private final String newFileName;
        private final String mimeType;
        private final String fileId;
        private int fileSize;
        private final int replyToMessageId;
        private volatile Supplier<Boolean> checker;
        private volatile boolean canceledByUser;
        private volatile SmartTempFile file;
        private volatile SmartTempFile thumbFile;
        private TgFile userThumb;
        private String thumb;
        private FileWorkObject fileWorkObject;

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
            this.fileWorkObject = fileManager.fileWorkObject(userId);
        }

        @Override
        public void run() {
            fileWorkObject.start();
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {})", userId, size, fileId);

            try {
                String ext = formatService.getExt(fileName, mimeType);
                file = tempFileService.createTempFile(userId, fileId, TAG, ext);
                fileManager.downloadFileByFileId(fileId, file);

                String fileName = createNewFileName(newFileName, ext);
                if (userThumb != null) {
                    thumbFile = thumbService.convertToThumb(userId, userThumb.getFileId(), userThumb.getFileName(), userThumb.getMimeType());
                } else if (StringUtils.isNotBlank(thumb)) {
                    thumbFile = tempFileService.createTempFile(userId, fileId, TAG, Format.JPG.getExt());
                    fileManager.downloadFileByFileId(thumb, thumbFile);
                }
                mediaMessageService.sendDocument(new SendDocument((long) userId, fileName, file.getFile())
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
                    fileWorkObject.stop();
                }
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void cancel() {
            if (!fileManager.cancelDownloading(fileId) && file != null) {
                file.smartDelete();
            }
            if (!fileManager.cancelDownloading(thumb) && thumbFile != null) {
                thumbFile.smartDelete();
            }
            if (canceledByUser) {
                renameQueueService.delete(jobId);
                fileWorkObject.stop();
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
