package ru.gadjini.any2any.service.thumb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.queue.ThumbQueueDao;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.ThumbQueueItem;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;

import java.util.List;

@Service
public class ThumbQueueService {

    private ThumbQueueDao thumbQueueDao;

    @Autowired
    public ThumbQueueService(ThumbQueueDao thumbQueueDao) {
        this.thumbQueueDao = thumbQueueDao;
    }

    public void resetProcessing() {
        thumbQueueDao.resetProcessing();
    }

    public ThumbQueueItem createProcessingItem(int userId, ThumbState thumbState) {
        ThumbQueueItem thumbQueueItem = new ThumbQueueItem();
        thumbQueueItem.setUserId(userId);

        TgFile thumbFile = new TgFile();
        thumbFile.setFileName(thumbState.getThumb().getFileName());
        thumbFile.setFileId(thumbState.getThumb().getFileId());
        thumbFile.setMimeType(thumbState.getThumb().getMimeType());
        thumbFile.setSize(thumbState.getThumb().getFileSize());
        thumbFile.setThumb(thumbState.getThumb().getThumb());
        thumbQueueItem.setThumb(thumbFile);
        thumbQueueItem.setReplyToMessageId(thumbState.getReplyToMessageId());

        TgFile file = new TgFile();
        file.setFileName(thumbState.getFile().getFileName());
        file.setFileId(thumbState.getFile().getFileId());
        file.setMimeType(thumbState.getFile().getMimeType());
        file.setSize(thumbState.getFile().getFileSize());
        file.setThumb(thumbState.getFile().getThumb());
        thumbQueueItem.setFile(file);

        thumbQueueItem.setStatus(ThumbQueueItem.Status.PROCESSING);

        int id = thumbQueueDao.create(thumbQueueItem);

        thumbQueueItem.setId(id);

        return thumbQueueItem;
    }

    public void setWaiting(int id) {
        thumbQueueDao.setWaiting(id);
    }

    public ThumbQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<ThumbQueueItem> poll = thumbQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<ThumbQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return thumbQueueDao.poll(weight, limit);
    }

    public ThumbQueueItem deleteWithReturning(int id) {
        return thumbQueueDao.deleteWithReturning(id);
    }

    public void delete(int id) {
        thumbQueueDao.delete(id);
    }

    public List<Integer> deleteByUserId(int userId) {
        return thumbQueueDao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return thumbQueueDao.exists(jobId);
    }
}
