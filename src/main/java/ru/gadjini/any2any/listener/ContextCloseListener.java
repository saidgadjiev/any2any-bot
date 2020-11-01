package ru.gadjini.any2any.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.job.ArchiverJobDelegate;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;

@Component
public class ContextCloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextCloseListener.class);

    private ArchiverJobDelegate archiverJob;

    private FileManager fileManager;

    private ThreadPoolTaskExecutor commonThreadPool;

    public ContextCloseListener(ArchiverJobDelegate archiverJob,
                                FileManager fileManager, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor commonThreadPool) {
        this.archiverJob = archiverJob;
        this.fileManager = fileManager;
        this.commonThreadPool = commonThreadPool;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            archiverJob.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown archiveService. " + e.getMessage(), e);
        }
        try {
            commonThreadPool.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown commonThreadPool. " + e.getMessage(), e);
        }
        try {
            fileManager.cancelDownloads();
        } catch (Throwable e) {
            LOGGER.error("Error cancel downloading telegramService. " + e.getMessage(), e);
        }
        LOGGER.debug("Shutdown success");
    }
}
