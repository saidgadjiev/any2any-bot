package ru.gadjini.any2any.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.utils.MimeTypeUtils;

import java.io.File;

@Service
public class RenameService {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public RenameService(TelegramService telegramService, FileService fileService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public File rename(RenameState renameState, String newFileName) {
        String ext = getExt(renameState.getMimeType(), renameState.getFileName());
        File file = createNewFile(newFileName, ext);

        return telegramService.downloadFileByFileId(renameState.getFileId(), file);
    }

    private String getExt(String mimeType, String fileName) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension)) {
            extension = extension.substring(1);
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }

        return extension;
    }

    private File createNewFile(String fileName, String ext) {
        if (StringUtils.isNotBlank(ext)) {
            String withExt = FilenameUtils.getExtension(fileName);

            if (StringUtils.isBlank(withExt)) {
                return fileService.createTempFile(fileName + "." + ext);
            } else {
                return fileService.createTempFile(fileName);
            }
        }

        return fileService.createTempFile(fileName);
    }
}
