package ru.gadjini.any2any.service.concurrent;

import java.util.Collection;
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

    public void complete(Collection<Integer> jobIds) {
        jobIds.forEach(this::complete);
    }

    public void cancel(int jobId) {
        Future<?> future = processing.get(jobId);
        if (future != null && (!future.isCancelled() || !future.isDone())) {
            future.cancel(true);
        }
    }

    public void cancelAndComplete(int jobId) {
        cancel(jobId);
        complete(jobId);
    }

    public boolean isCanceled(int jobId) {
        if (processing.containsKey(jobId)) {
            return processing.get(jobId).isCancelled();
        }

        return false;
    }

    public void cancel(List<Integer> ids) {
        ids.forEach(this::cancel);
    }

    public void cancelAndComplete(List<Integer> ids) {
        ids.forEach(integer -> {
            cancel(integer);
            complete(integer);
        });
    }

    public interface Job extends Runnable {
        int getId();
    }
}
