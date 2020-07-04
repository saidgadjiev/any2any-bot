package ru.gadjini.any2any.service.concurrent;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SmartExecutorService implements ExecutorService {

    private TaskCompleteListener taskCompleteListener;

    private ExecutorService executorService;

    public SmartExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        return executorService.submit(task);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        return executorService.submit(task, result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        return executorService.submit(task);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                command.run();
                taskCompleteListener.taskCompleted();
            }
        });
    }

    public void setTaskCompleteListener(TaskCompleteListener taskCompleteListener) {
        this.taskCompleteListener = taskCompleteListener;
    }

    public interface TaskCompleteListener {

        void taskCompleted();
    }
}
