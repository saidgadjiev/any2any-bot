package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.archive.ArchiveQueueService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private ThreadPoolExecutor executor;

    private TelegramService telegramService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private ArchiveQueueService archiveQueueService;

    @Autowired
    public ArchiveService(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                          TelegramService telegramService, LocalisationService localisationService,
                          @Qualifier("limits") MessageService messageService, UserService userService,
                          ArchiveQueueService archiveQueueService) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.telegramService = telegramService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.archiveQueueService = archiveQueueService;
    }

    @PostConstruct
    public void init() {
        archiveQueueService.resetProcessing();
    }

    @Autowired
    public void setExecutor(@Qualifier("archiveTaskExecutor") ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public void rejectRenameTask(ArchiveTask renameTask) {
        archiveQueueService.setWaiting(renameTask.jobId);
    }

    public ArchiveTask getTask() {
        synchronized (this) {
            ArchiveQueueItem peek = archiveQueueService.peek();

            return new ArchiveTask(peek.getId(), peek.getFiles(), peek.getUserId(), peek.getType(),
                    userService.getLocaleOrDefault(peek.getUserId()));
        }
    }

    public SmartTempFile createArchive(int userId, List<File> files, Format archiveFormat) {
        Locale locale = userService.getLocaleOrDefault(userId);
        SmartTempFile archive = fileService.getTempFile(
                Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), archiveFormat.getExt())
        );
        ArchiveDevice archiveDevice = getCandidate(archiveFormat, locale);
        archiveDevice.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());

        return archive;
    }

    public void createArchive(int userId, List<Any2AnyFile> any2AnyFiles, Format format, Locale locale) {
        LOGGER.debug("Start createArchive({}, {})", userId, format);
        normalizeFileNames(any2AnyFiles);

        ArchiveQueueItem item = archiveQueueService.createProcessingItem(userId, any2AnyFiles, format);

        executor.execute(new ArchiveTask(item.getId(), item.getFiles(), item.getUserId(), item.getType(), locale));
    }

    private void sendResult(int userId, File archive) {
        messageService.sendDocument(new SendDocument((long) userId, archive));
    }

    private List<SmartTempFile> downloadFiles(List<TgFile> tgFiles) {
        List<SmartTempFile> files = new ArrayList<>();

        for (TgFile tgFile : tgFiles) {
            SmartTempFile file = fileService.createTempFile(tgFile.getFileName());
            telegramService.downloadFileByFileId(tgFile.getFileId(), file.getFile());
            files.add(file);
        }

        return files;
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

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_TYPE_UNSUPPORTED, new Object[]{format}, locale));
    }

    public class ArchiveTask implements Runnable {

        private int jobId;

        private List<TgFile> archiveFiles;

        private int userId;

        private Format type;

        private Locale locale;

        private ArchiveTask(int jobId, List<TgFile> archiveFiles, int userId, Format type, Locale locale) {
            this.jobId = jobId;
            this.archiveFiles = archiveFiles;
            this.userId = userId;
            this.type = type;
            this.locale = locale;
        }

        @Override
        public void run() {
            List<SmartTempFile> files = downloadFiles(archiveFiles);
            try {
                SmartTempFile archive = fileService.getTempFile(
                        Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), type.getExt())
                );
                try {
                    ArchiveDevice archiveDevice = getCandidate(type, locale);
                    archiveDevice.zip(files.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
                    sendResult(userId, archive.getFile());
                    LOGGER.debug("Finish createArchive({}, {})", userId, type);
                } catch (Exception ex) {
                    messageService.sendErrorMessage(userId, locale);
                    throw ex;
                } finally {
                    archive.smartDelete();
                }
            } finally {
                files.forEach(SmartTempFile::smartDelete);
            }
        }
    }
}
