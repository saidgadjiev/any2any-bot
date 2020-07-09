package ru.gadjini.any2any.service.unzip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
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

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Component
@Conditional(WindowsCondition.class)
@Qualifier("zip")
public class ZipLibUnzipDevice extends BaseUnzipDevice {

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
    public String unzip(String fileHeader, String archivePath, String dir) {
        ZipFile zipFile = new ZipFile(archivePath);
        try {
            zipFile.extractFile(fileHeader, dir);

            return dir + File.separator + fileHeader.replaceAll("[/\\\\]", Matcher.quoteReplacement(File.separator));
        } catch (ZipException e) {
            throw new UnzipException(e);
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
