package ru.gadjini.any2any.service.unzip;

import com.github.junrar.Archive;
import com.github.junrar.ExtractDestination;
import com.github.junrar.LocalFolderExtractor;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.vfs2.provider.rar.FileSystem;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RarUnzipper extends BaseUnzipper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RarUnzipper.class);

    private TelegramService telegramService;

    private FileService fileService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public RarUnzipper(TelegramService telegramService, FileService fileService,
                       LocalisationService localisationService, UserService userService) {
        super(Set.of(Format.RAR));
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public UnzipResult unzip(int userId, String fileId, Format format) {
        LOGGER.debug("Rar unzip: " + fileId);
        File rar = telegramService.downloadFileByFileId(fileId, format.getExt());

        try {
            File rootDir = fileService.createTempDir(fileId);
            Archive archive = check(new Archive(new FileVolumeManager(rar)), userService.getLocale(userId));
            LocalFolderExtractor lfe = new LocalFolderExtractor(rootDir, new FileSystem());
            List<File> files = extractArchiveTo(archive, lfe);

            return new UnzipResult(files, rootDir);
        } catch (Exception e) {
            throw new UnzipException(e);
        } finally {
            FileUtils.deleteQuietly(rar);
        }
    }

    private static List<File> extractArchiveTo(final Archive arch, final ExtractDestination destination) throws IOException, RarException {
        if (arch.isEncrypted()) {
            arch.close();
            return new ArrayList<>();
        }

        List<File> extractedFiles = new ArrayList<>();
        try (arch) {
            for (final FileHeader fh : arch) {
                final File file = tryToExtract(destination, arch, fh);
                if (file != null) {
                    extractedFiles.add(file);
                }
            }
        }

        return extractedFiles;
    }

    private static File tryToExtract(
            final ExtractDestination destination,
            final Archive arch,
            final FileHeader fileHeader
    ) throws IOException, RarException {
        if (fileHeader.isEncrypted()) {
            return null;
        }
        if (fileHeader.isDirectory()) {
            return destination.createDirectory(fileHeader);
        } else {
            return destination.extract(arch, fileHeader);
        }
    }

    private Archive check(Archive archive, Locale locale) throws RarException {
        if (archive.isEncrypted()) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_ENCRYPTED, locale));
        }

        return archive;
    }
}
