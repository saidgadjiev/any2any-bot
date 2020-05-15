package ru.gadjini.any2any.service.unzip;

import com.github.junrar.Junrar;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.exception.UnzipException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.io.File;
import java.util.List;
import java.util.Set;

@Component
public class RarUnzipper extends BaseUnzipper {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public RarUnzipper(TelegramService telegramService, FileService fileService) {
        super(Set.of(Format.RAR));
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public UnzipResult unzip(String fileId, Format format) {
        File rar = telegramService.downloadFileByFileId(fileId, format.getExt());

        try {
            File rootDir = fileService.createTempDir(fileId);
            List<File> files = Junrar.extract(rar, rootDir);

            return new UnzipResult(files, rootDir);
        } catch (Exception e) {
            throw new UnzipException(e);
        } finally {
            FileUtils.deleteQuietly(rar);
        }
    }
}
