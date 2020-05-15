package ru.gadjini.any2any.service.unzip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ZipUnzipper extends BaseUnzipper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUnzipper.class);

    private TelegramService telegramService;

    private FileService fileService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public ZipUnzipper(TelegramService telegramService, FileService fileService,
                       LocalisationService localisationService, UserService userService) {
        super(Set.of(Format.ZIP));
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public UnzipResult unzip(int userId, String fileId, Format format) {
        LOGGER.debug("Zip unzip: " + fileId);
        File zip = telegramService.downloadFileByFileId(fileId, format.getExt());

        try {
            File rootDir = fileService.createTempDir(fileId);

            ZipFile zipFile = checkZip(new ZipFile(zip), userService.getLocale(userId));
            zipFile.extractAll(rootDir.getAbsolutePath());

            List<File> files = new ArrayList<>();
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                if (fileHeader.isDirectory()) {
                    continue;
                }
                files.add(new File(rootDir, fileHeader.getFileName()));
            }

            return new UnzipResult(files, rootDir);
        } catch (ZipException e) {
            throw new UnzipException(e);
        } finally {
            FileUtils.deleteQuietly(zip);
        }
    }

    private ZipFile checkZip(ZipFile zipFile, Locale locale) throws ZipException {
        if (zipFile.isEncrypted()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_ENCRYPTED, locale));
        }
        if (!zipFile.isValidZipFile()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_INVALID, locale));
        }

        return zipFile;
    }
}
