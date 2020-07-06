package ru.gadjini.any2any.service.queue.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.queue.UnzipQueueDao;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.service.conversion.api.Format;

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

    public int createProcessingUnzipItem(int userId, String fileId, Format format) {
        UnzipQueueItem queueItem = new UnzipQueueItem();
        queueItem.setUserId(userId);
        queueItem.setType(format);
        queueItem.setItemType(UnzipQueueItem.ItemType.UNZIP);

        TgFile file = new TgFile();
        file.setFileId(fileId);
        queueItem.setFile(file);

        queueItem.setStatus(UnzipQueueItem.Status.PROCESSING);

        return unzipQueueDao.create(queueItem);
    }

    public UnzipQueueItem createProcessingExtractFileItem(int userId, int extractFileId) {
        UnzipQueueItem item = new UnzipQueueItem();
        item.setUserId(userId);
        item.setExtractFileId(extractFileId);
        item.setItemType(UnzipQueueItem.ItemType.EXTRACT_FILE);
        item.setStatus(UnzipQueueItem.Status.PROCESSING);

        int jobId = unzipQueueDao.create(item);
        item.setId(jobId);

        return item;
    }

    public void setWaiting(int id) {
        unzipQueueDao.setWaiting(id);
    }

    public UnzipQueueItem peek() {
        return unzipQueueDao.poll();
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
