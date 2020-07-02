package ru.gadjini.any2any.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.message.MessageService;

import java.io.File;
import java.util.Locale;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private TelegramService telegramService;

    private TempFileService fileService;

    private FormatService formatService;

    private MessageService messageService;

    private CommonJobExecutor commonJobExecutor;

    @Autowired
    public RenameService(TelegramService telegramService, TempFileService fileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, CommonJobExecutor commonJobExecutor) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.commonJobExecutor = commonJobExecutor;
    }

    public void rename(long chatId, RenameState renameState, String newFileName, Locale locale) {
        commonJobExecutor.addJob(() -> {
            String ext = formatService.getExt(renameState.getFile().getFileName(), renameState.getFile().getMimeType());
            SmartTempFile file = createNewFile(newFileName, ext);
            telegramService.downloadFileByFileId(renameState.getFile().getFileId(), file.getFile());
            try {
                sendMessage(chatId, renameState.getReplyMessageId(), file.getFile());
                LOGGER.debug("Rename success for " + chatId + " new file name " + newFileName);
            } catch (Exception ex) {
                messageService.sendErrorMessage(chatId, locale);
                throw ex;
            } finally {
                file.smartDelete();
            }
        });
    }

    private SmartTempFile createNewFile(String fileName, String ext) {
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
            messageService.sendDocument(new SendDocument(chatId, renamed).setReplyToMessageId(replyMessageId));
        } finally {
            FileUtils.deleteQuietly(renamed);
        }
    }
}
