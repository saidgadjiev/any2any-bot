package ru.gadjini.any2any.job;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveDevice;
import ru.gadjini.any2any.service.archive.ArchiveMessageBuilder;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.archive.ArchiveStep;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.queue.ArchiveQueueService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.exception.DownloadingException;
import ru.gadjini.telegram.smart.bot.commons.exception.FloodWaitException;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileWorkObject;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ArchiverJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private SmartExecutorService executor;

    private FileManager fileManager;

    private LocalisationService localisationService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private UserService userService;

    private ArchiveQueueService queueService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    private FileLimitProperties fileLimitProperties;

    private ProgressManager progressManager;

    @Autowired
    public ArchiverJob(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                       FileManager fileManager, LocalisationService localisationService,
                       @Qualifier("messageLimits") MessageService messageService,
                       @Qualifier("forceMedia") MediaMessageService mediaMessageService, UserService userService,
                       ArchiveQueueService queueService, CommandStateService commandStateService,
                       InlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder archiveMessageBuilder,
                       FileLimitProperties fileLimitProperties, ProgressManager progressManager) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.userService = userService;
        this.queueService = queueService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.archiveMessageBuilder = archiveMessageBuilder;
        this.fileLimitProperties = fileLimitProperties;
        this.progressManager = progressManager;
    }

    @Autowired
    public void setExecutor(@Qualifier("archiveTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        queueService.resetProcessing();
        pushJobs();
    }

    public void reject(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    @Scheduled(fixedDelay = 5000)
    public void pushJobs() {
        ThreadPoolExecutor heavyExecutor = executor.getExecutor(SmartExecutorService.JobWeight.HEAVY);
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<ArchiveQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.HEAVY, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push heavy jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ArchiveTask(queueItem)));
        }
        ThreadPoolExecutor lightExecutor = executor.getExecutor(SmartExecutorService.JobWeight.LIGHT);
        if (lightExecutor.getActiveCount() < lightExecutor.getCorePoolSize()) {
            Collection<ArchiveQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, lightExecutor.getCorePoolSize() - lightExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ArchiveTask(queueItem)));
        }
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<ArchiveQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs to heavy threads({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new ArchiveTask(queueItem), SmartExecutorService.JobWeight.HEAVY));
        }
    }

    public void removeAndCancelCurrentTask(long chatId) {
        ArchiveQueueItem item = queueService.deleteByUserId((int) chatId);
        if (item != null && !executor.cancelAndComplete(item.getId(), true)) {
            fileManager.fileWorkObject(chatId, item.getTotalFileSize()).stop();
        }
    }

    public void leave(long chatId) {
        ArchiveQueueItem queueItem = queueService.deleteByUserId((int) chatId);

        if (queueItem != null && !executor.cancelAndComplete(queueItem.getId(), true)) {
            fileManager.fileWorkObject(chatId, queueItem.getTotalFileSize()).stop();
        }
        commandStateService.deleteState(chatId, FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!queueService.exists(jobId)) {
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
                ArchiveQueueItem archiveQueueItem = queueService.deleteWithReturning(jobId);
                if (archiveQueueItem != null) {
                    fileManager.fileWorkObject(chatId, archiveQueueItem.getTotalFileSize()).stop();
                }
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }


    private ArchiveDevice getCandidate(Format format, Locale locale) {
        for (ArchiveDevice archiveDevice : archiveDevices) {
            if (archiveDevice.accept(format)) {
                return archiveDevice;
            }
        }

        LOGGER.warn("No candidate({})", format);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_TYPE_UNSUPPORTED, new Object[]{format}, locale));
    }

    private Progress progressFilesDownloading(int count, int current, long chatId, int processMessageId, int jobId) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(chatId);
        progress.setProgressMessageId(processMessageId);
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProgressMessage(count, current, ArchiveStep.DOWNLOADING, Lang.PYTHON, locale));

        if (count == current) {
            String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(ArchiveStep.ARCHIVE_CREATION, Lang.JAVA, locale);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "10 " + seconds));
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale));
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale));

        return progress;
    }

    private Progress progressArchiveUploading(long chatId, int processMessageId, int jobId, long fileSize) {
        if (progressManager.isShowingUploadingProgress(fileSize)) {
            Locale locale = userService.getLocaleOrDefault((int) chatId);
            Progress progress = new Progress();
            progress.setLocale(locale.getLanguage());
            progress.setChatId(chatId);
            progress.setProgressMessageId(processMessageId);
            progress.setProgressMessage(archiveMessageBuilder.buildArchiveProcessMessage(ArchiveStep.UPLOADING, Lang.PYTHON, locale));

            String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(ArchiveStep.COMPLETED, Lang.JAVA, locale);
            progress.setAfterProgressCompletionMessage(completionMessage);

            progress.setProgressReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale));

            return progress;
        } else {
            return null;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    public class ArchiveTask implements SmartExecutorService.Job {

        private static final String TAG = "archive";

        private int jobId;

        private int progressMessageId;

        private List<TgFile> archiveFiles;

        private int userId;

        private Format type;

        private long totalFileSize;

        private volatile Supplier<Boolean> checker;

        private Queue<SmartTempFile> files = new LinkedBlockingQueue<>();

        private volatile SmartTempFile archive;

        private volatile boolean canceledByUser;

        private FileWorkObject fileWorkObject;

        private ArchiveTask(ArchiveQueueItem item) {
            this.jobId = item.getId();
            this.archiveFiles = item.getFiles();
            this.totalFileSize = item.getTotalFileSize();
            this.userId = item.getUserId();
            this.type = item.getType();
            this.progressMessageId = item.getProgressMessageId();
            this.fileWorkObject = fileManager.fileWorkObject(userId, totalFileSize);
        }

        @Override
        public void execute() {
            fileWorkObject.start();
            boolean success = false;
            String size;
            try {
                size = MemoryUtils.humanReadableByteCount(totalFileSize);
                LOGGER.debug("Start({}, {}, {})", userId, size, type);
                DownloadResult downloadResult = downloadFiles(userId, progressMessageId, jobId, archiveFiles);
                Locale locale = userService.getLocaleOrDefault(userId);

                archive = fileService.getTempFile(userId, TAG, type.getExt());
                ArchiveDevice archiveDevice = getCandidate(type, locale);
                archiveDevice.zip(files.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
                renameFiles(archiveDevice, archive.getAbsolutePath(), downloadResult.originalFileNames, downloadResult.downloadedNames);

                String fileName = Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), type.getExt());
                mediaMessageService.sendDocument(new SendDocument((long) userId, fileName, archive.getFile())
                        .setProgress(progressArchiveUploading(userId, progressMessageId, jobId, archive.length())));

                LOGGER.debug("Finish({}, {}, {})", userId, size, type);
                success = true;
            } catch (Throwable ex) {
                if (checker == null || !checker.get()) {
                    int downloadingExceptionIndexOf = ExceptionUtils.indexOfThrowable(ex, DownloadingException.class);
                    int floodWaitExceptionIndexOf = ExceptionUtils.indexOfThrowable(ex, FloodWaitException.class);
                    if (downloadingExceptionIndexOf != -1 || floodWaitExceptionIndexOf != -1) {
                        LOGGER.error(ex.getMessage());
                        queueService.setWaiting(jobId);
                        updateProgressMessageAfterFloodWait(jobId);
                    } else {
                        throw ex;
                    }
                }
            } finally {
                if (checker == null || !checker.get()) {
                    executor.complete(jobId);
                    files.forEach(SmartTempFile::smartDelete);
                    files.clear();

                    if (archive != null) {
                        archive.smartDelete();
                    }

                    if (success) {
                        queueService.delete(jobId);
                        fileWorkObject.stop();
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
            archiveFiles.forEach(tgFile -> fileManager.cancelDownloading(tgFile.getFileId()));
            files.forEach(smartTempFile -> {
                if (!fileManager.cancelUploading(smartTempFile.getAbsolutePath())) {
                    smartTempFile.smartDelete();
                }
            });

            if (archive != null && !fileManager.cancelUploading(archive.getAbsolutePath())) {
                archive.smartDelete();
            }
            if (canceledByUser) {
                fileWorkObject.stop();
                LOGGER.debug("Canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(totalFileSize));
            }
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public Supplier<Boolean> getCancelChecker() {
            return checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return totalFileSize > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        @Override
        public long getChatId() {
            return userId;
        }

        @Override
        public int getProgressMessageId() {
            return progressMessageId;
        }

        private void renameFiles(ArchiveDevice archiveDevice, String archive,
                                 Map<Integer, String> originalFileNames, Map<Integer, String> downloadedNames) {
            for (Map.Entry<Integer, String> entry : downloadedNames.entrySet()) {
                archiveDevice.rename(archive, entry.getValue(), originalFileNames.get(entry.getKey()));
            }
        }

        private void updateProgressMessageAfterFloodWait(int id) {
            Locale locale = userService.getLocaleOrDefault(id);
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_AWAITING_PROCESSING, locale);

            messageService.editMessage(new EditMessageText((long) userId, progressMessageId, message)
                    .setNoLogging(true)
                    .setReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale)));
        }

        private DownloadResult downloadFiles(int userId, int progressMessageId, int jobId, List<TgFile> tgFiles) {
            DownloadResult downloadResult = new DownloadResult();

            int i = 1;
            for (TgFile tgFile : tgFiles) {
                SmartTempFile file = fileService.createTempFile(userId, tgFile.getFileId(), TAG, FilenameUtils.getExtension(tgFile.getFileName()));
                fileManager.forceDownloadFileByFileId(tgFile.getFileId(), tgFile.getSize(),
                        progressFilesDownloading(tgFiles.size(), i, userId, progressMessageId, jobId), file);
                downloadResult.originalFileNames.put(i, tgFile.getFileName());
                downloadResult.downloadedNames.put(i++, file.getName());
                files.add(file);
            }

            return downloadResult;
        }

        private class DownloadResult {

            private Map<Integer, String> originalFileNames = new HashMap<>();

            private Map<Integer, String> downloadedNames = new HashMap<>();
        }
    }
}
