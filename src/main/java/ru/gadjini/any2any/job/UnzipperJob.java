package ru.gadjini.any2any.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class UnzipperJob {

    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    public UnzipperJob(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void addJob(Runnable job) {
        taskExecutor.execute(job);
    }
}
