package ru.gadjini.any2any.job;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveDevice;
import ru.gadjini.any2any.service.archive.ArchiveMessageBuilder;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.archive.ArchiveStep;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.common.TgConstants;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorker;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ArchiveQueueWorkerFactory implements QueueWorkerFactory<ArchiveQueueItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private FileManager fileManager;

    private LocalisationService localisationService;

    private MediaMessageService mediaMessageService;

    private UserService userService;

    private SmartInlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    @Autowired
    public ArchiveQueueWorkerFactory(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                                     FileManager fileManager, LocalisationService localisationService,
                                     @Qualifier("forceMedia") MediaMessageService mediaMessageService, UserService userService,
                                     SmartInlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder archiveMessageBuilder) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.localisationService = localisationService;
        this.mediaMessageService = mediaMessageService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.archiveMessageBuilder = archiveMessageBuilder;
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

    private Progress progressFilesDownloading(ArchiveQueueItem queueItem, int count, int current) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProgressMessage(queueItem, count, current, ArchiveStep.DOWNLOADING, locale));

        if (count == current) {
            String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.ARCHIVE_CREATION, locale);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "10 " + seconds));
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    private Progress progressArchiveUploading(ArchiveQueueItem queueItem) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.UPLOADING, locale));

        String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.COMPLETED, locale);
        progress.setAfterProgressCompletionMessage(completionMessage);

        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    @Override
    public QueueWorker createWorker(ArchiveQueueItem item) {
        return new ArchiveQueueWorker(item);
    }

    public class ArchiveQueueWorker implements QueueWorker {

        private static final String TAG = "archive";

        private final ArchiveQueueItem item;

        private volatile SmartTempFile archive;

        private ArchiveQueueWorker(ArchiveQueueItem item) {
            this.item = item;
        }

        @Override
        public void execute() {
            String size = MemoryUtils.humanReadableByteCount(item.getTotalFileSize());
            LOGGER.debug("Start({}, {}, {})", item.getUserId(), size, item.getType());

            Locale locale = userService.getLocaleOrDefault(item.getUserId());

            archive = fileService.getTempFile(item.getUserId(), TAG, item.getType().getExt());
            ArchiveDevice archiveDevice = getCandidate(item.getType(), locale);
            boolean addedAllFiles = downloadFiles(item, item.getFiles(), (file, downloadedFileName, originalFileName) -> {
                try {
                    archiveDevice.zip(List.of(file.getAbsolutePath()), archive.getAbsolutePath());
                    archiveDevice.rename(archive.getAbsolutePath(), downloadedFileName, originalFileName);

                    if (archive.length() > TgConstants.LARGE_FILE_SIZE) {
                        return false;
                    }
                } finally {
                    file.smartDelete();
                }

                return true;
            });

            String fileName = Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), item.getType().getExt());
            mediaMessageService.sendDocument(SendDocument.builder().chatId(String.valueOf(item.getUserId())).document(new InputFile(archive.getFile(), fileName))
                            .caption(addedAllFiles ? null : localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_SIZE_EXCEEDED, locale)).build(),
                    progressArchiveUploading(item));

            LOGGER.debug("Finish({}, {}, {})", item.getId(), size, item.getType());
        }

        @Override
        public void cancel() {
            item.getFiles().forEach(tgFile -> fileManager.cancelDownloading(tgFile.getFileId(), tgFile.getSize()));

            if (archive != null && !fileManager.cancelUploading(archive.getAbsolutePath())) {
                archive.smartDelete();
            }
        }

        @Override
        public void finish() {
            if (archive != null) {
                archive.smartDelete();
            }
        }

        private boolean downloadFiles(ArchiveQueueItem queueItem, List<TgFile> tgFiles, DownloadCallback downloadCallback) {
            int i = 1;
            for (TgFile tgFile : tgFiles) {
                SmartTempFile file = fileService.createTempFile(queueItem.getUserId(), tgFile.getFileId(), TAG, FilenameUtils.getExtension(tgFile.getFileName()));
                fileManager.downloadFileByFileId(tgFile.getFileId(), tgFile.getSize(),
                        progressFilesDownloading(queueItem, tgFiles.size(), i), file);
                boolean continueDownload = downloadCallback.onDownload(file, file.getName(), tgFile.getFileName());
                if (!continueDownload) {
                    return false;
                }
            }

            return true;
        }
    }

    private interface DownloadCallback {
        boolean onDownload(SmartTempFile file, String downloadedFileName, String originalFileName);
    }
}
