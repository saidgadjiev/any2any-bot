package ru.gadjini.any2any.service.file;

import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWorkObject {

    private long chatId;

    private FileLimitsDao fileLimitsDao;

    private StopWatch stopWatch = new StopWatch();

    private List<Consumer<FileWorkObject>> stopListeners = new ArrayList<>();

    public FileWorkObject(long chatId, FileLimitsDao fileLimitsDao) {
        this.chatId = chatId;
        this.fileLimitsDao = fileLimitsDao;
    }

    public FileWorkObject(long chatId, FileLimitsDao fileLimitsDao, StopWatch stopWatch) {
        this.chatId = chatId;
        this.fileLimitsDao = fileLimitsDao;
        this.stopWatch = stopWatch;
    }

    public StopWatch getWatch() {
        return stopWatch;
    }

    public long getChatId() {
        return chatId;
    }

    public void resume() {
        stopWatch.resume();
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
            fileLimitsDao.setInputFileTtl(chatId, stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
            stopListeners.forEach(fileWorkObjectConsumer -> fileWorkObjectConsumer.accept(this));
            stopListeners.clear();
        }
    }

    public void addStopListener(Consumer<FileWorkObject> callback) {
        stopListeners.add(callback);
    }
}
