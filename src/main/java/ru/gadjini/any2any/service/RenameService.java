package ru.gadjini.any2any.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.service.converter.impl.FormatService;

import java.io.File;

@Service
public class RenameService {

    private TelegramService telegramService;

    private FileService fileService;

    private FormatService formatService;

    @Autowired
    public RenameService(TelegramService telegramService, FileService fileService, FormatService formatService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
    }

    public File rename(RenameState renameState, String newFileName) {
        String ext = formatService.getExt(renameState.getFileName(), renameState.getMimeType());
        File file = createNewFile(newFileName, ext);

        return telegramService.downloadFileByFileId(renameState.getFileId(), file);
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
