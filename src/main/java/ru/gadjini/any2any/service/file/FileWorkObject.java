package ru.gadjini.any2any.service.file;

import ru.gadjini.any2any.service.message.TelegramMediaServiceProvider;

import java.util.concurrent.TimeUnit;

public class FileWorkObject {

    private static final int TTL = 3 * 60;

    private long chatId;

    private long fileSize;

    private FileLimitsDao fileLimitsDao;

    public FileWorkObject(long chatId, long fileSize, FileLimitsDao fileLimitsDao) {
        this.chatId = chatId;
        this.fileSize = fileSize;
        this.fileLimitsDao = fileLimitsDao;
    }

    public long getChatId() {
        return chatId;
    }

    public void start() {
        if (fileSize > 0 && fileSize < TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT) {
            return;
        }
        fileLimitsDao.setState(chatId, InputFileState.State.PROCESSING);
    }

    public void stop() {
        if (fileSize > 0 && fileSize < TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT) {
            return;
        }
        fileLimitsDao.setState(chatId, InputFileState.State.COMPLETED);
        fileLimitsDao.setInputFileTtl(chatId, TTL, TimeUnit.SECONDS);
    }
}
