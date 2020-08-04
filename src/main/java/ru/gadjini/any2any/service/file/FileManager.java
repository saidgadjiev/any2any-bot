package ru.gadjini.any2any.service.file;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

    public void inputFile(long chatId, int messageId) {
        boolean hasKey = fileLimitsDao.hasInputFile(chatId);
        if (!hasKey) {
            fileLimitsDao.setInputFile(chatId, messageId);
        } else {
            Long ttl = fileLimitsDao.getInputFileTtl(chatId);

            if (ttl == null) {
                Integer replyToMessageId = fileLimitsDao.getMessageId(chatId);
                //TODO:
                Objects.requireNonNull(replyToMessageId, "reply to message id can't be null");
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT, locale)).setReplyToMessageId(replyToMessageId);
            } else {
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT_TTL, new Object[] {ttl}, locale));
            }
        }
    }

    public void downloadFileByFileId(long chatId, String fileId, SmartTempFile outputFile) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            telegramService.downloadFileByFileId(fileId, outputFile);
        } finally {
            stopWatch.stop();
            fileLimitsDao.setInputFileTtl(0, stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
    }

    public boolean cancelDownloading(String fileId) {
        return telegramService.cancelDownloading(fileId);
    }

    public void cancelDownloads() {
        telegramService.cancelDownloads();
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        telegramService.restoreFileIfNeed(filePath, fileId);
    }
}
