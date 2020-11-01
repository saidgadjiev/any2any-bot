package ru.gadjini.any2any.job;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueJobDelegate;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Component
public class ArchiverJobDelegate implements QueueJobDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private FileManager fileManager;

    private LocalisationService localisationService;

    private MediaMessageService mediaMessageService;

    private UserService userService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    @Autowired
    public ArchiverJobDelegate(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                               FileManager fileManager, LocalisationService localisationService,
                               @Qualifier("forceMedia") MediaMessageService mediaMessageService, UserService userService,
                               CommandStateService commandStateService,
                               InlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder archiveMessageBuilder) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.fileManager = fileManager;
        this.localisationService = localisationService;
        this.mediaMessageService = mediaMessageService;
        this.userService = userService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.archiveMessageBuilder = archiveMessageBuilder;
    }

    @Override
    public WorkerTaskDelegate mapWorker(QueueItem queueItem) {
        return new ArchiveTask((ArchiveQueueItem) queueItem);
    }

    @Override
    public void currentTasksRemoved(int userId) {
        commandStateService.deleteState(userId, FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
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

    private Progress progressFilesDownloading(ArchiveQueueItem queueItem, int count, int current, long fileSize) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProgressMessage(queueItem, count, current, ArchiveStep.DOWNLOADING, fileSize, Lang.PYTHON, locale));

        if (count == current) {
            String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.ARCHIVE_CREATION, fileSize, Lang.JAVA, locale);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "10 " + seconds));
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale));
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    private Progress progressArchiveUploading(ArchiveQueueItem queueItem, long fileSize) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.UPLOADING, fileSize, Lang.PYTHON, locale));

        String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.COMPLETED, fileSize, Lang.JAVA, locale);
        progress.setAfterProgressCompletionMessage(completionMessage);

        progress.setProgressReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    public class ArchiveTask implements WorkerTaskDelegate {

        private static final String TAG = "archive";

        private final ArchiveQueueItem item;

        private Queue<SmartTempFile> files = new LinkedBlockingQueue<>();

        private volatile SmartTempFile archive;

        private ArchiveTask(ArchiveQueueItem item) {
            this.item = item;
        }

        @Override
        public void execute() {
            String size = MemoryUtils.humanReadableByteCount(item.getTotalFileSize());
            LOGGER.debug("Start({}, {}, {})", item.getUserId(), size, item.getType());
            DownloadResult downloadResult = downloadFiles(item, item.getFiles());
            Locale locale = userService.getLocaleOrDefault(item.getUserId());

            archive = fileService.getTempFile(item.getUserId(), TAG, item.getType().getExt());
            ArchiveDevice archiveDevice = getCandidate(item.getType(), locale);
            archiveDevice.zip(files.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
            renameFiles(archiveDevice, archive.getAbsolutePath(), downloadResult.originalFileNames, downloadResult.downloadedNames);

            String fileName = Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), item.getType().getExt());
            mediaMessageService.sendDocument(new SendDocument((long) item.getUserId(), fileName, archive.getFile())
                    .setProgress(progressArchiveUploading(item, archive.length())));

            LOGGER.debug("Finish({}, {}, {})", item.getId(), size, item.getType());
        }

        @Override
        public void cancel() {
            item.getFiles().forEach(tgFile -> fileManager.cancelDownloading(tgFile.getFileId()));
            files.forEach(smartTempFile -> {
                if (!fileManager.cancelUploading(smartTempFile.getAbsolutePath())) {
                    smartTempFile.smartDelete();
                }
            });

            if (archive != null && !fileManager.cancelUploading(archive.getAbsolutePath())) {
                archive.smartDelete();
            }
        }

        @Override
        public String getWaitingMessage(QueueItem queueItem, Locale locale) {
            return archiveMessageBuilder.buildArchiveProcessMessage((ArchiveQueueItem) queueItem, ArchiveStep.WAITING, queueItem.getSize(), Lang.JAVA, locale);
        }

        @Override
        public InlineKeyboardMarkup getWaitingKeyboard(QueueItem queueItem, Locale locale) {
            return inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale);
        }

        @Override
        public void finish() {
            files.forEach(SmartTempFile::smartDelete);
            files.clear();

            if (archive != null) {
                archive.smartDelete();
            }
        }

        @Override
        public boolean shouldBeDeletedAfterCompleted() {
            return true;
        }

        private void renameFiles(ArchiveDevice archiveDevice, String archive,
                                 Map<Integer, String> originalFileNames, Map<Integer, String> downloadedNames) {
            for (Map.Entry<Integer, String> entry : downloadedNames.entrySet()) {
                archiveDevice.rename(archive, entry.getValue(), originalFileNames.get(entry.getKey()));
            }
        }

        private DownloadResult downloadFiles(ArchiveQueueItem queueItem, List<TgFile> tgFiles) {
            DownloadResult downloadResult = new DownloadResult();

            int i = 1;
            for (TgFile tgFile : tgFiles) {
                SmartTempFile file = fileService.createTempFile(queueItem.getUserId(), tgFile.getFileId(), TAG, FilenameUtils.getExtension(tgFile.getFileName()));
                fileManager.forceDownloadFileByFileId(tgFile.getFileId(), tgFile.getSize(),
                        progressFilesDownloading(queueItem, tgFiles.size(), i, tgFile.getSize()), file);
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
