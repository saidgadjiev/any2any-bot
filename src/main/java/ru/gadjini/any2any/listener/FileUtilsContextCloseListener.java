package ru.gadjini.any2any.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class FileUtilsContextCloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsContextCloseListener.class);

    private ThreadPoolTaskExecutor commonThreadPool;

    public FileUtilsContextCloseListener(@Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor commonThreadPool) {
        this.commonThreadPool = commonThreadPool;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            commonThreadPool.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown commonThreadPool. " + e.getMessage(), e);
        }
        LOGGER.debug("Shutdown success");
    }
}
