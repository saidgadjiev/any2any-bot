package ru.gadjini.any2any.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.converter.impl.FormatService;

import java.io.File;

@Service
public class RenameService {

    private TelegramService telegramService;

    private FileService fileService;

    private FormatService formatService;

    private MessageService messageService;

    private CommonJobExecutor commonJobExecutor;

    @Autowired
    public RenameService(TelegramService telegramService, FileService fileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, CommonJobExecutor commonJobExecutor) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.commonJobExecutor = commonJobExecutor;
    }

    public void rename(long chatId, RenameState renameState, String newFileName) {
        commonJobExecutor.addJob(() -> {
            String ext = formatService.getExt(renameState.getFileName(), renameState.getMimeType());
            File file = createNewFile(newFileName, ext);
            File renamed = telegramService.downloadFileByFileId(renameState.getFileId(), file);
            try {
                sendMessage(chatId, renameState.getReplyMessageId(), renamed);
            } finally {
                FileUtils.deleteQuietly(renamed);
            }
        });
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

    private void sendMessage(long chatId, int replyMessageId, File renamed) {
        try {
            messageService.sendDocument(new SendFileContext(chatId, renamed).replyMessageId(replyMessageId));
        } finally {
            FileUtils.deleteQuietly(renamed);
        }
    }
}
