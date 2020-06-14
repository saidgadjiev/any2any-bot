package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class CommonJobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonJobExecutor.class);

    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    public CommonJobExecutor(@Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        LOGGER.debug("Common job started");
    }

    public void addJob(Runnable job) {
        taskExecutor.execute(job);
    }
}
