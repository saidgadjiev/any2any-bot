package ru.gadjini.any2any.service.concurrent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SmartExecutorService {

    private ExecutorService executorService;

    private final Map<Integer, Future<?>> processing = new ConcurrentHashMap<>();

    public SmartExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void execute(Job job) {
        Future<?> submit = executorService.submit(job);
        processing.put(job.getId(), submit);
    }

    public void complete(int jobId) {
        processing.remove(jobId);
    }

    public void cancel(int jobId) {
        Future<?> future = processing.get(jobId);
        if (future != null && (!future.isCancelled() || !future.isDone())) {
            future.cancel(true);
        }
    }

    public void cancel(List<Integer> ids) {
        ids.forEach(this::cancel);
    }

    public interface Job extends Runnable {
        int getId();
    }
}
