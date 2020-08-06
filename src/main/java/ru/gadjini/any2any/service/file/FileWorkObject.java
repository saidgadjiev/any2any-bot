package ru.gadjini.any2any.service.file;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class FileWorkObject {

    private static final int MIN_10 = 4 * 60;

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
            if (seconds < 15) {
                seconds  = 15;
            } else if (seconds > MIN_10) {
                seconds = MIN_10;
            }

            fileLimitsDao.setInputFileTtl(chatId, seconds, TimeUnit.SECONDS);
        }
    }
}
