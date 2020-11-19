package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.cleaner.GarbageFileCollector;

import java.time.LocalDateTime;

@Component
public class GarbageFilesCollectorJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GarbageFilesCollectorJob.class);

    private GarbageFileCollector fileCollector;

    @Autowired
    public GarbageFilesCollectorJob(GarbageFileCollector fileCollector) {
        this.fileCollector = fileCollector;
    }

    @Scheduled(cron = "0 0 */10 * * *")
    public void run() {
        LOGGER.debug("Start({})", LocalDateTime.now());
        int clean = fileCollector.clean();
        LOGGER.debug("Finish({}, {})", clean, LocalDateTime.now());
    }
}
