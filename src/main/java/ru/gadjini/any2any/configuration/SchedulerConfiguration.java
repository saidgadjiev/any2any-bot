package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.conversion.ConversionService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    public static final int QUEUE_SIZE = 60;

    @Bean
    public TaskScheduler jobsThreadPoolTaskScheduler(UserService userService) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(3);
        threadPoolTaskScheduler.setThreadNamePrefix("JobsThreadPoolTaskScheduler");
        threadPoolTaskScheduler.setErrorHandler(ex -> {
            if (userService.deadlock(ex)) {
                LOGGER.debug("Blocked user " + ((TelegramRequestException) ex).getChatId());
            } else {
                LOGGER.error(ex.getMessage(), ex);
            }
        });

        LOGGER.debug("Jobs thread pool scheduler initialized with pool size: {}", threadPoolTaskScheduler.getPoolSize());

        return threadPoolTaskScheduler;
    }

    @Bean
    @Qualifier("conversionTaskExecutor")
    public ThreadPoolExecutor conversionTaskExecutor(ConversionService conversionService) {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                (r, executor) -> conversionService.rejectTask((ConversionService.ConversionTask) r)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = conversionService.getTask();
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        LOGGER.debug("Rename thread pool executor initialized with pool size: {}", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }

    @Bean
    @Qualifier("commonTaskExecutor")
    public ThreadPoolTaskExecutor commonExecutorService() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setQueueCapacity(QUEUE_SIZE);
        taskExecutor.setThreadNamePrefix("CommonExecutor");
        taskExecutor.initialize();

        LOGGER.debug("Common thread pool executor initialized with pool size: {}", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }

    @Bean
    @Qualifier("renameTaskExecutor")
    public ThreadPoolExecutor renameTaskExecutor(RenameService renameService) {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                (r, executor) -> renameService.rejectRenameTask((RenameService.RenameTask) r)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = renameService.getTask();
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        LOGGER.debug("Rename thread pool executor initialized with pool size: {}", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }

    @Bean
    @Qualifier("archiveTaskExecutor")
    public ThreadPoolExecutor archiveTaskExecutor(ArchiveService archiveService) {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                (r, executor) -> archiveService.rejectRenameTask((ArchiveService.ArchiveTask) r)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = archiveService.getTask();
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        LOGGER.debug("Archive thread pool executor initialized with pool size: {}", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }
}
