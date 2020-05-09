package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    @Bean
    public TaskScheduler jobsThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.setThreadNamePrefix("JobsThreadPoolTaskScheduler");
        threadPoolTaskScheduler.setErrorHandler(throwable -> LOGGER.error(throwable.getMessage(), throwable));

        LOGGER.debug("Jobs thread pool scheduler initialized with pool size: {}", threadPoolTaskScheduler.getPoolSize());

        return threadPoolTaskScheduler;
    }

    @Bean
    public ThreadPoolTaskExecutor reminderExecutorService() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2 * Runtime.getRuntime().availableProcessors());
        taskExecutor.setMaxPoolSize(2 * Runtime.getRuntime().availableProcessors() + 2);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.setThreadNamePrefix("Any2AnyExecutor");
        taskExecutor.initialize();

        LOGGER.debug("Any2Any thread pool executor initialized with pool size: {}", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }
}
