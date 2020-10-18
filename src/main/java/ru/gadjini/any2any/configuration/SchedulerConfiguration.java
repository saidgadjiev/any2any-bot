package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.gadjini.any2any.job.ArchiverJob;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService.JobWeight.HEAVY;
import static ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService.JobWeight.LIGHT;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private ArchiverJob archiverJob;

    @Autowired
    public void setArchiveService(ArchiverJob archiverJob) {
        this.archiverJob = archiverJob;
    }

    @Bean
    @Qualifier("commonTaskExecutor")
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setThreadNamePrefix("CommonTaskExecutor");
        taskExecutor.initialize();
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);

        LOGGER.debug("Common thread pool({})", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }

    @Bean
    @Qualifier("archiveTaskExecutor")
    public SmartExecutorService archiveTaskExecutor(UserService userService, FileManager fileManager,
                                                    @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService) {
        SmartExecutorService executorService = new SmartExecutorService(messageService, localisationService, fileManager, userService);
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new SynchronousQueue<>());

        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new SynchronousQueue<>());

        LOGGER.debug("Archive light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Archive heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
        executorService.setRejectJobHandler(LIGHT, job -> archiverJob.reject(job));
        executorService.setRejectJobHandler(HEAVY, job -> archiverJob.reject(job));

        return executorService;
    }
}
