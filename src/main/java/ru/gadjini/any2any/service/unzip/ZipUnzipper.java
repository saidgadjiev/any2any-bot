package ru.gadjini.any2any.service.unzip;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ZipUnzipper extends BaseUnzipper {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public ZipUnzipper(TelegramService telegramService, FileService fileService) {
        super(Set.of(Format.ZIP));
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public UnzipResult unzip(String fileId, Format format) {
        File zip = telegramService.downloadFileByFileId(fileId, format.getExt());

        try {
            File rootDir = fileService.createTempDir(fileId);

            ZipFile zipFile = new ZipFile(zip);
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
}
