package ru.gadjini.any2any.service.unzip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.condition.WindowsCondition;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Conditional(WindowsCondition.class)
@Qualifier("zip")
public class ZipLibUnzipDevice extends BaseUnzipDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipLibUnzipDevice.class);

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public ZipLibUnzipDevice(LocalisationService localisationService, UserService userService) {
        super(Set.of(Format.ZIP));
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void unzip(int userId, String in, String out) {
        try {
            ZipFile zipFile = checkZip(new ZipFile(in), userService.getLocaleOrDefault(userId));
            zipFile.extractAll(out);
        } catch (ZipException e) {
            throw new UnzipException(e);
        }
    }

    @Override
    public List<ZipFileHeader> getZipFiles(String zipFile) {
        ZipFile zip = new ZipFile(zipFile);
        try {
            List<FileHeader> fileHeaders = zip.getFileHeaders().stream().filter(fileHeader -> !fileHeader.isDirectory()).collect(Collectors.toList());

            return fileHeaders.stream().map(fileHeader -> new ZipFileHeader(fileHeader.getFileName(), fileHeader.getUncompressedSize())).collect(Collectors.toList());
        } catch (ZipException e) {
            throw new UnzipException(e);
        }
    }

    @Override
    public void unzip(String fileHeader, String archivePath, String out) {
        throw new UnsupportedOperationException();
    }

    private ZipFile checkZip(ZipFile zipFile, Locale locale) throws ZipException {
        if (zipFile.isEncrypted()) {
            LOGGER.warn("Encrypted");
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_ENCRYPTED, locale));
        }
        if (!zipFile.isValidZipFile()) {
            LOGGER.warn("Invalid");
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_INVALID, locale));
        }

        return zipFile;
    }
}
