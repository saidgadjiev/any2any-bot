package ru.gadjini.any2any.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class CommonJobExecutor {

    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    public CommonJobExecutor(@Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void addJob(Runnable job) {
        taskExecutor.execute(job);
    }
}
