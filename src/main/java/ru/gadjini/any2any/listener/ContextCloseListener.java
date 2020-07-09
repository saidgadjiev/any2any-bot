package ru.gadjini.any2any.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.conversion.ConvertionService;
import ru.gadjini.any2any.service.unzip.UnzipService;

@Component
public class ContextCloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextCloseListener.class);

    private ConvertionService conversionService;

    private RenameService renameService;

    private ArchiveService archiveService;

    private UnzipService unzipService;

    private TelegramService telegramService;

    private ThreadPoolTaskExecutor commonThreadPool;

    public ContextCloseListener(ConvertionService conversionService, RenameService renameService,
                                ArchiveService archiveService, UnzipService unzipService,
                                TelegramService telegramService, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor commonThreadPool) {
        this.conversionService = conversionService;
        this.renameService = renameService;
        this.archiveService = archiveService;
        this.unzipService = unzipService;
        this.telegramService = telegramService;
        this.commonThreadPool = commonThreadPool;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            conversionService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown conversionService. " + e.getMessage(), e);
        }
        try {
            renameService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown renameService. " + e.getMessage(), e);
        }
        try {
            archiveService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown archiveService. " + e.getMessage(), e);
        }
        try {
            unzipService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown unzipService. " + e.getMessage(), e);
        }
        try {
            commonThreadPool.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown commonThreadPool. " + e.getMessage(), e);
        }
        try {
            telegramService.cancelDownloads();
        } catch (Throwable e) {
            LOGGER.error("Error cancel downloading telegramService. " + e.getMessage(), e);
        }
        LOGGER.debug("Shutdown success");
    }
}
