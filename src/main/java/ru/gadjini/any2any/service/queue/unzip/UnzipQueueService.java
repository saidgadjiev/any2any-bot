package ru.gadjini.any2any.service.queue.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.queue.UnzipQueueDao;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.service.conversion.api.Format;

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

    public int createProcessingItem(int userId, String fileId, Format format) {
        UnzipQueueItem renameQueueItem = new UnzipQueueItem();
        renameQueueItem.setUserId(userId);
        renameQueueItem.setType(format);

        TgFile file = new TgFile();
        file.setFileId(fileId);
        renameQueueItem.setFile(file);

        renameQueueItem.setStatus(UnzipQueueItem.Status.PROCESSING);

        return unzipQueueDao.create(renameQueueItem);
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
}
