package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.queue.ArchiveQueueService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Any2AnyFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileWorkObject;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private static final String TAG = "archive";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private SmartExecutorService executor;

    private FileManager fileManager;

    private LocalisationService localisationService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private UserService userService;

    private ArchiveQueueService archiveQueueService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    private ProgressManager progressManager;

    @Autowired
    public ArchiveService(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                          FileManager fileManager, LocalisationService localisationService,
                          @Qualifier("messageLimits") MessageService messageService,
                          @Qualifier("mediaLimits") MediaMessageService mediaMessageService, UserService userService,
                          ArchiveQueueService archiveQueueService, CommandStateService commandStateService,
                          InlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder archiveMessageBuilder, ProgressManager progressManager) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.userService = userService;
        this.archiveQueueService = archiveQueueService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.archiveMessageBuilder = archiveMessageBuilder;
        this.progressManager = progressManager;
    }

    @PostConstruct
    public void init() {
        archiveQueueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("archiveTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectTask(SmartExecutorService.Job job) {
        archiveQueueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    public ArchiveTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            ArchiveQueueItem peek = archiveQueueService.poll(weight);

            if (peek != null) {
                return new ArchiveTask(peek);
            }

            return null;
        }
    }

    public SmartTempFile createArchive(int userId, List<File> files, Format archiveFormat) {
        Locale locale = userService.getLocaleOrDefault(userId);
        SmartTempFile archive = fileService.createTempFile(userId, TAG, archiveFormat.getExt());
        ArchiveDevice archiveDevice = getCandidate(archiveFormat, locale);
        archiveDevice.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());

        return archive;
    }

    public void removeAndCancelCurrentTasks(long chatId) {
        List<Integer> ids = archiveQueueService.deleteByUserId((int) chatId);
        executor.cancelAndComplete(ids, false);
    }

    public void leave(long chatId) {
        List<Integer> ids = archiveQueueService.deleteByUserId((int) chatId);

        if (!ids.isEmpty()) {
            LOGGER.debug("Cancel by chat id({}, {})", chatId, ids.size());
        }
        try {
            executor.cancelAndComplete(ids, true);
        } finally {
            fileManager.fileWorkObject(chatId, -1).stop();
        }
        commandStateService.deleteState(chatId, CommandNames.ARCHIVE_COMMAND_NAME);
    }

    public void createArchive(int userId, ArchiveState archiveState, Format format) {
        normalizeFileNames(archiveState.getFiles());

        ArchiveQueueItem item = archiveQueueService.createProcessingItem(userId, archiveState.getFiles(), format);
        startArchiveCreating(userId, item.getId(), item.getTotalFileSize(), message -> {
            archiveQueueService.setProgressMessageId(item.getId(), message.getMessageId());
            item.setProgressMessageId(message.getMessageId());
            fileManager.setInputFilePending(userId, null, null, item.getTotalFileSize(), TAG);
            executor.execute(new ArchiveTask(item));
        });
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!archiveQueueService.exists(jobId)) {
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
                archiveQueueService.delete(jobId);
                fileManager.fileWorkObject(chatId, -1).stop();
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    private void startArchiveCreating(int userId, int jobId, long fileSize, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(userId);
        if (progressManager.isShowingDownloadingProgress(fileSize)) {
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_AWAITING_PROCESSING, locale);
            messageService.sendMessage(new SendMessage((long) userId, message)
                    .setReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale)), callback);
        } else {
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_PROCESSING, locale);
            messageService.sendMessage(new SendMessage((long) userId, message)
                    .setReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale)), callback);
        }
    }

    private void normalizeFileNames(List<Any2AnyFile> any2AnyFiles) {
        Set<String> uniqueFileNames = new HashSet<>();

        for (Any2AnyFile any2AnyFile : any2AnyFiles) {
            if (!uniqueFileNames.add(any2AnyFile.getFileName())) {
                int index = 1;
                while (true) {
                    String fileName = normalizeFileName(any2AnyFile.getFileName(), index++);
                    if (uniqueFileNames.add(fileName)) {
                        any2AnyFile.setFileName(fileName);
                        break;
                    }
                }
            }
        }
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<ArchiveQueueItem> tasks = archiveQueueService.poll(jobWeight, 1);
        for (ArchiveQueueItem item : tasks) {
            executor.execute(new ArchiveTask(item));
            LOGGER.debug("Push task");
        }
    }

    private String normalizeFileName(String fileName, int index) {
        String ext = FilenameUtils.getExtension(fileName);
        if (StringUtils.isBlank(ext)) {
            return fileName + " (" + index + ")";
        }
        String name = FilenameUtils.getBaseName(fileName);

        return name + " (" + index + ")." + ext;
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

    private Progress progressArchiveCreation(long chatId, int processMessageId, int jobId) {
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
    }

    public void shutdown() {
        executor.shutdown();
    }

    public class ArchiveTask implements SmartExecutorService.Job {

        public static final String TAG = "archive";

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
        public void run() {
            fileWorkObject.start();
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
                        .setProgress(progressArchiveCreation(userId, progressMessageId, jobId)));

                LOGGER.debug("Finish({}, {}, {})", userId, size, type);
            } catch (Exception ex) {
                if (checker == null || !checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                if (checker == null || !checker.get()) {
                    executor.complete(jobId);
                    archiveQueueService.delete(jobId);
                    files.forEach(SmartTempFile::smartDelete);
                    files.clear();

                    if (archive != null) {
                        archive.smartDelete();
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
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return totalFileSize > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void renameFiles(ArchiveDevice archiveDevice, String archive,
                                 Map<Integer, String> originalFileNames, Map<Integer, String> downloadedNames) {
            for (Map.Entry<Integer, String> entry : downloadedNames.entrySet()) {
                archiveDevice.rename(archive, entry.getValue(), originalFileNames.get(entry.getKey()));
            }
        }

        private DownloadResult downloadFiles(int userId, int progressMessageId, int jobId, List<TgFile> tgFiles) {
            DownloadResult downloadResult = new DownloadResult();

            int i = 1;
            for (TgFile tgFile : tgFiles) {
                SmartTempFile file = fileService.createTempFile(userId, tgFile.getFileId(), TAG, FilenameUtils.getExtension(tgFile.getFileName()));
                fileManager.downloadFileByFileId(tgFile.getFileId(), tgFile.getSize(),
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
