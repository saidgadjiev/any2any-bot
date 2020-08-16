package ru.gadjini.any2any.service.file;

import java.util.concurrent.TimeUnit;

public class FileWorkObject {

    private static final int TTL = 3 * 60;

    private long chatId;

    private FileLimitsDao fileLimitsDao;

    public FileWorkObject(long chatId, FileLimitsDao fileLimitsDao) {
        this.chatId = chatId;
        this.fileLimitsDao = fileLimitsDao;
    }

    public long getChatId() {
        return chatId;
    }

    public void start() {
        fileLimitsDao.setState(chatId, InputFileState.State.PROCESSING);
    }

    public void stop() {
        fileLimitsDao.setState(chatId, InputFileState.State.COMPLETED);
        fileLimitsDao.setInputFileTtl(chatId, TTL, TimeUnit.SECONDS);
    }
}
