package ru.gadjini.any2any.service.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.object.Progress;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;

import java.util.Locale;

@Service
public class FileManager {

    private TelegramService telegramService;

    private FileLimitsDao fileLimitsDao;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public FileManager(TelegramService telegramService, FileLimitsDao fileLimitsDao,
                       LocalisationService localisationService, UserService userService) {
        this.telegramService = telegramService;
        this.fileLimitsDao = fileLimitsDao;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void setInputFilePending(long chatId, Integer replyToMessageId) {
        fileLimitsDao.setInputFile(chatId, new InputFileState(replyToMessageId));
    }

    public void resetLimits(long chatId) {
        fileLimitsDao.deleteInputFile(chatId);
    }

    public void inputFile(long chatId) {
        InputFileState inputFileState = fileLimitsDao.getInputFile(chatId);
        if (inputFileState != null) {
            Long ttl = fileLimitsDao.getInputFileTtl(chatId);

            if (ttl == null || ttl == -1) {
                Integer replyToMessageId = inputFileState.getReplyToMessageId();
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT, locale)).setReplyToMessageId(replyToMessageId);
            } else {
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT_TTL, new Object[]{ttl}, locale));
            }
        }
    }

    public void downloadFileByFileId(String fileId, SmartTempFile outputFile) {
        telegramService.downloadFileByFileId(fileId, outputFile);
    }

    public void downloadFileByFileId(String fileId, long fileSize, Progress progress, SmartTempFile outputFile) {
        telegramService.downloadFileByFileId(fileId, fileSize, progress, outputFile);
    }

    public FileWorkObject fileWorkObject(long chatId) {
        return new FileWorkObject(chatId, fileLimitsDao);
    }

    public boolean cancelDownloading(String fileId) {
        return telegramService.cancelDownloading(fileId);
    }

    public boolean cancelUploading(String filePath) {
        return telegramService.cancelUploading(filePath);
    }

    public void cancelDownloads() {
        telegramService.cancelDownloads();
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        telegramService.restoreFileIfNeed(filePath, fileId);
    }
}
