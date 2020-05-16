package ru.gadjini.any2any.service.unzip;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.job.UnzipperJob;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.*;
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

    private Set<ZipService> unzippers;

    private LocalisationService localisationService;

    private UnzipperJob unzipperJob;

    private MessageService messageService;

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public UnzipperService(Set<ZipService> unzippers, LocalisationService localisationService,
                           UnzipperJob unzipperJob, @Qualifier("limits") MessageService messageService,
                           TelegramService telegramService, FileService fileService) {
        this.unzippers = unzippers;
        this.localisationService = localisationService;
        this.unzipperJob = unzipperJob;
        this.messageService = messageService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public void unzip(int userId, String fileId, Format format, Locale locale) {
        LOGGER.debug(format + " unzip: " + fileId);
        ZipService zipService = getCandidate(format, locale);

        unzipperJob.addJob(() -> {
            File in = telegramService.downloadFileByFileId(fileId, format.getExt());

            try {
                File out = fileService.createTempDir(fileId);
                try {
                    zipService.unzip(userId, in.getAbsolutePath(), out.getAbsolutePath());
                    List<File> files = new ArrayList<>();
                    ExFileUtils.list(out.getAbsolutePath(), files);
                    sendFiles(userId, files);
                } finally {
                    FileUtils.deleteQuietly(out);
                }
            } finally {
                FileUtils.deleteQuietly(in);
            }
        });
    }

    private void sendFiles(int userId, List<File> files) {
        for (File file : files) {
            messageService.sendDocument(new SendFileContext(userId, file));
        }
    }

    private ZipService getCandidate(Format format, Locale locale) {
        for (ZipService unzipper : unzippers) {
            if (unzipper.accept(format)) {
                return unzipper;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }
}
