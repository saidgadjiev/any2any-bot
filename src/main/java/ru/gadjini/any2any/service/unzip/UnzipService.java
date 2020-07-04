package ru.gadjini.any2any.service.unzip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.unzip.UnzipQueueService;
import ru.gadjini.any2any.utils.ExFileUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class UnzipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipService.class);

    private Set<UnzipDevice> unzipDevices;

    private LocalisationService localisationService;

    private ThreadPoolExecutor executor;

    private MessageService messageService;

    private TelegramService telegramService;

    private TempFileService fileService;

    private UnzipQueueService queueService;

    private UserService userService;

    @Autowired
    public UnzipService(Set<UnzipDevice> unzipDevices, LocalisationService localisationService,
                        @Qualifier("limits") MessageService messageService,
                        TelegramService telegramService, TempFileService fileService,
                        UnzipQueueService queueService, UserService userService) {
        this.unzipDevices = unzipDevices;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.queueService = queueService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        queueService.resetProcessing();
    }

    @Autowired
    public void setExecutor(@Qualifier("unzipTaskExecutor") ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public void rejectTask(UnzipTask unzipTask) {
        queueService.setWaiting(unzipTask.jobId);
    }

    public UnzipTask getTask() {
        synchronized (this) {
            UnzipQueueItem peek = queueService.peek();

            UnzipDevice unzipDevice = getCandidate(peek.getType());
            return new UnzipTask(peek.getId(), peek.getUserId(), peek.getFile().getFileId(), peek.getType(),
                    userService.getLocaleOrDefault(peek.getUserId()), unzipDevice);
        }
    }

    public void unzip(int userId, String fileId, Format format, Locale locale) {
        LOGGER.debug("Start unzip({}, {}, {})", format, fileId, userId);
        UnzipDevice unzipDevice = checkCandidate(format, locale);
        int id = queueService.createProcessingItem(userId, fileId, format);

        executor.execute(new UnzipTask(id, userId, fileId, format, locale, unzipDevice));
    }

    private void sendFiles(int userId, List<File> files, Locale locale) {
        if (files.isEmpty()) {
            sendNoFilesMessage(userId, locale);
        } else {
            for (File file : files) {
                messageService.sendDocument(new SendDocument((long) userId, file));
            }
        }
    }

    private UnzipDevice getCandidate(Format format) {
        for (UnzipDevice unzipDevice : unzipDevices) {
            if (unzipDevice.accept(format)) {
                return unzipDevice;
            }
        }

        throw new IllegalArgumentException("Candidate not found for " + format + ". Wtf?");
    }

    private UnzipDevice checkCandidate(Format format, Locale locale) {
        for (UnzipDevice unzipDevice : unzipDevices) {
            if (unzipDevice.accept(format)) {
                return unzipDevice;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }

    private void sendNoFilesMessage(int userId, Locale locale) {
        messageService.sendMessage(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_NO_FILES, locale)));
    }

    public class UnzipTask implements Runnable {

        private final int jobId;
        private final int userId;
        private final String fileId;
        private final Format format;
        private final Locale locale;
        private UnzipDevice unzipDevice;

        private UnzipTask(int jobId, int userId, String fileId, Format format, Locale locale, UnzipDevice unzipDevice) {
            this.jobId = jobId;
            this.userId = userId;
            this.fileId = fileId;
            this.format = format;
            this.locale = locale;
            this.unzipDevice = unzipDevice;
        }

        @Override
        public void run() {
            SmartTempFile in = telegramService.downloadFileByFileId(fileId, format.getExt());

            try {
                SmartTempFile out = fileService.createTempDir(fileId);
                try {
                    unzipDevice.unzip(userId, in.getAbsolutePath(), out.getAbsolutePath());
                    List<File> files = new ArrayList<>();
                    ExFileUtils.list(out.getAbsolutePath(), files);
                    sendFiles(userId, files, locale);
                    LOGGER.debug("Finish unzip({}, {}, {})", format, fileId, userId);
                } catch (Exception ex) {
                    messageService.sendErrorMessage(userId, locale);
                    throw ex;
                } finally {
                    out.smartDelete();
                }
            } finally {
                queueService.delete(jobId);
                in.smartDelete();
            }
        }
    }
}
