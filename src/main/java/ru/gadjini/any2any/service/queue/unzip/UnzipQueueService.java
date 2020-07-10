package ru.gadjini.any2any.service.queue.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.queue.UnzipQueueDao;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;

import java.util.List;

@Service
public class UnzipQueueService {

    private UnzipQueueDao unzipQueueDao;

    @Autowired
    public UnzipQueueService(UnzipQueueDao unzipQueueDao) {
        this.unzipQueueDao = unzipQueueDao;
    }

    public void resetProcessing() {
        unzipQueueDao.resetProcessing();
    }

    public UnzipQueueItem createProcessingUnzipItem(int userId, Any2AnyFile any2AnyFile) {
        UnzipQueueItem queueItem = new UnzipQueueItem();
        queueItem.setUserId(userId);
        queueItem.setType(any2AnyFile.getFormat());
        queueItem.setItemType(UnzipQueueItem.ItemType.UNZIP);

        TgFile file = new TgFile();
        file.setFileId(any2AnyFile.getFileId());
        file.setSize(any2AnyFile.getFileSize());
        queueItem.setFile(file);

        queueItem.setStatus(UnzipQueueItem.Status.PROCESSING);

        int id = unzipQueueDao.create(queueItem);
        queueItem.setId(id);

        return queueItem;
    }

    public UnzipQueueItem createProcessingExtractFileItem(int userId, int extractFileId, long extractFileSize) {
        UnzipQueueItem item = new UnzipQueueItem();
        item.setUserId(userId);
        item.setExtractFileId(extractFileId);
        item.setItemType(UnzipQueueItem.ItemType.EXTRACT_FILE);
        item.setStatus(UnzipQueueItem.Status.PROCESSING);
        item.setExtractFileSize(extractFileSize);

        int jobId = unzipQueueDao.create(item);
        item.setId(jobId);

        return item;
    }

    public void setWaiting(int id, int messageId) {
        unzipQueueDao.setWaiting(id, messageId);
    }

    public void setWaiting(int id) {
        unzipQueueDao.setWaiting(id);
    }

    public UnzipQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<UnzipQueueItem> poll = unzipQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<UnzipQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return unzipQueueDao.poll(weight, limit);
    }

    public void delete(int id) {
        unzipQueueDao.delete(id);
    }

    public List<Integer> deleteByUserId(int userId) {
        return unzipQueueDao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return unzipQueueDao.exists(jobId);
    }
}
