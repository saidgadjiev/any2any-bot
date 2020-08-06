package ru.gadjini.any2any.service.file;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class FileWorkObject {

    private static final int MAX_LIMIT = 3 * 60;

    private static final int MIN_LIMIT = 15;

    private long chatId;

    private FileLimitsDao fileLimitsDao;

    private StopWatch stopWatch = new StopWatch();

    public FileWorkObject(long chatId, FileLimitsDao fileLimitsDao) {
        this.chatId = chatId;
        this.fileLimitsDao = fileLimitsDao;
    }

    public long getChatId() {
        return chatId;
    }

    public void start() {
        if (stopWatch.isSuspended()) {
            stopWatch.start();
        } else {
            stopWatch.start();
        }
        fileLimitsDao.setState(chatId, InputFileState.State.PROCESSING);
    }

    public void stop() {
        if (stopWatch.isStarted()) {
            stopWatch.stop();

            fileLimitsDao.setState(chatId, InputFileState.State.COMPLETED);
            long seconds = stopWatch.getTime(TimeUnit.SECONDS);
            if (seconds < MIN_LIMIT) {
                seconds  = MIN_LIMIT;
            } else if (seconds > MAX_LIMIT) {
                seconds = MAX_LIMIT;
            }

            fileLimitsDao.setInputFileTtl(chatId, seconds, TimeUnit.SECONDS);
        }
    }
}
