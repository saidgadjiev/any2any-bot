package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class FileUtilsSchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsSchedulerConfiguration.class);

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
}
