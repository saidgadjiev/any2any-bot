package ru.gadjini.any2any.service.unzip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.utils.ExFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UnzipperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipperService.class);

    private Set<UnzipDevice> unzippers;

    private LocalisationService localisationService;

    private CommonJobExecutor unzipperJob;

    private MessageService messageService;

    private TelegramService telegramService;

    private TempFileService fileService;

    @Autowired
    public UnzipperService(Set<UnzipDevice> unzippers, LocalisationService localisationService,
                           CommonJobExecutor unzipperJob, @Qualifier("limits") MessageService messageService,
                           TelegramService telegramService, TempFileService fileService) {
        this.unzippers = unzippers;
        this.localisationService = localisationService;
        this.unzipperJob = unzipperJob;
        this.messageService = messageService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public void unzip(int userId, String fileId, Format format, Locale locale) {
        LOGGER.debug(format + " unzip: " + fileId);
        UnzipDevice zipService = getCandidate(format, locale);

        unzipperJob.addJob(() -> {
            SmartTempFile in = telegramService.downloadFileByFileId(fileId, format.getExt());

            try {
                SmartTempFile out = fileService.createTempDir(fileId);
                try {
                    zipService.unzip(userId, in.getAbsolutePath(), out.getAbsolutePath());
                    List<File> files = new ArrayList<>();
                    ExFileUtils.list(out.getAbsolutePath(), files);
                    sendFiles(userId, files, locale);
                } catch (Exception ex) {
                    messageService.sendErrorMessage(userId, locale);
                    throw ex;
                } finally {
                    out.smartDelete();
                }
            } finally {
                in.smartDelete();
            }
        });
    }

    private void sendFiles(int userId, List<File> files, Locale locale) {
        if (files.isEmpty()) {
            sendNoFilesMessage(userId, locale);
        } else {
            for (File file : files) {
                messageService.sendDocument(new SendFileContext(userId, file));
            }
        }
    }

    private UnzipDevice getCandidate(Format format, Locale locale) {
        for (UnzipDevice unzipper : unzippers) {
            if (unzipper.accept(format)) {
                return unzipper;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }

    private void sendNoFilesMessage(int userId, Locale locale) {
        messageService.sendMessage(new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_NO_FILES, locale)));
    }
}
