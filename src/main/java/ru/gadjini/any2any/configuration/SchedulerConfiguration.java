package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.telegram.smart.bot.commons.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService.Job;
import static ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService.JobWeight.HEAVY;
import static ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService.JobWeight.LIGHT;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private ArchiveService archiveService;

    private UserService userService;

    @Autowired
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public TaskScheduler jobsThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        threadPoolTaskScheduler.setThreadNamePrefix("JobsThreadPoolTaskScheduler");
        threadPoolTaskScheduler.setErrorHandler(ex -> {
            if (userService.deadlock(ex)) {
                LOGGER.debug("Blocked user({})", ((TelegramApiRequestException) ex).getChatId());
            } else {
                LOGGER.error(ex.getMessage(), ex);
            }
        });

        LOGGER.debug("Jobs thread pool scheduler initialized with pool size({})", threadPoolTaskScheduler.getPoolSize());

        return threadPoolTaskScheduler;
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
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(4, 4,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    archiveService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = archiveService.getTask(LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(4, 4,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    archiveService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = archiveService.getTask(HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Archive light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Archive heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }
}
