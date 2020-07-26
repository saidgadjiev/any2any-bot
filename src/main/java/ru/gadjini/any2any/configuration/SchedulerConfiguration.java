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
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.service.RenameService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.ConvertionService;
import ru.gadjini.any2any.service.thumb.ThumbService;
import ru.gadjini.any2any.service.unzip.UnzipService;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.any2any.service.concurrent.SmartExecutorService.Job;
import static ru.gadjini.any2any.service.concurrent.SmartExecutorService.JobWeight.HEAVY;
import static ru.gadjini.any2any.service.concurrent.SmartExecutorService.JobWeight.LIGHT;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private static final int LIGHT_QUEUE_SIZE = 10;

    private static final int HEAVY_QUEUE_SIZE = 10;

    private static final int THREADS_KEEP_ALIVE = 0;

    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static final int LIGHT_THREADS_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int HEAVY_THREADS_COUNT = Runtime.getRuntime().availableProcessors();

    private ConvertionService conversionService;

    private UnzipService unzipService;

    private ArchiveService archiveService;

    private RenameService renameService;

    private UserService userService;

    private ThumbService thumbService;

    @Autowired
    public void setConversionService(ConvertionService conversionService) {
        this.conversionService = conversionService;
    }

    @Autowired
    public void setUnzipService(UnzipService unzipService) {
        this.unzipService = unzipService;
    }

    @Autowired
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Autowired
    public void setRenameService(RenameService renameService) {
        this.renameService = renameService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setThumbService(ThumbService thumbService) {
        this.thumbService = thumbService;
    }

    @Bean
    public TaskScheduler jobsThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
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
    @Qualifier("conversionTaskExecutor")
    public SmartExecutorService conversionTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    conversionService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = conversionService.getTask(LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    conversionService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = conversionService.getTask(HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Conversion light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Conversion heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }

    @Bean
    @Qualifier("renameTaskExecutor")
    public SmartExecutorService renameTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    renameService.rejectRenameTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = renameService.getTask(LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    renameService.rejectRenameTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = renameService.getTask(HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Rename light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Rename heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }


    @Bean
    @Qualifier("thumbTaskExecutor")
    public SmartExecutorService thumbTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    thumbService.reject((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = thumbService.getTask(LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    thumbService.reject((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = thumbService.getTask(HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Thumb light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Thumb heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }

    @Bean
    @Qualifier("archiveTaskExecutor")
    public SmartExecutorService archiveTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    archiveService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = archiveService.getTask(LIGHT);
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    archiveService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = archiveService.getTask(HEAVY);
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        LOGGER.debug("Archive light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Archive heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }

    @Bean
    @Qualifier("unzipTaskExecutor")
    public SmartExecutorService unzipTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    unzipService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = unzipService.getTask(LIGHT);
                if (poll != null) {
                    execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    unzipService.rejectTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Runnable poll = unzipService.getTask(HEAVY);
                if (poll != null) {
                    execute(poll);
                }
            }
        };

        LOGGER.debug("Unzip light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Unzip heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }
}
