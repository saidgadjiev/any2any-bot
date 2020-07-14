package ru.gadjini.any2any.service.queue.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.dao.queue.RenameQueueDao;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;

import java.util.List;

@Service
public class RenameQueueService {

    private RenameQueueDao renameQueueDao;

    @Autowired
    public RenameQueueService(RenameQueueDao renameQueueDao) {
        this.renameQueueDao = renameQueueDao;
    }

    public void resetProcessing() {
        renameQueueDao.resetProcessing();
    }

    public RenameQueueItem createProcessingItem(int userId, RenameState renameState, String newFileName) {
        RenameQueueItem renameQueueItem = new RenameQueueItem();
        renameQueueItem.setUserId(userId);
        renameQueueItem.setNewFileName(newFileName);
        renameQueueItem.setReplyToMessageId(renameState.getReplyMessageId());

        TgFile file = new TgFile();
        file.setFileName(renameState.getFile().getFileName());
        file.setFileId(renameState.getFile().getFileId());
        file.setMimeType(renameState.getFile().getMimeType());
        file.setSize(renameState.getFile().getFileSize());
        renameQueueItem.setFile(file);

        renameQueueItem.setStatus(RenameQueueItem.Status.PROCESSING);

        int id = renameQueueDao.create(renameQueueItem);

        renameQueueItem.setId(id);

        return renameQueueItem;
    }

    public void setWaiting(int id) {
        renameQueueDao.setWaiting(id);
    }

    public RenameQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<RenameQueueItem> poll = renameQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<RenameQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return renameQueueDao.poll(weight, limit);
    }

    public RenameQueueItem deleteWithReturning(int id) {
        return renameQueueDao.deleteWithReturning(id);
    }

    public void delete(int id) {
        renameQueueDao.delete(id);
    }

    public List<Integer> deleteByUserId(int userId) {
        return renameQueueDao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return renameQueueDao.exists(jobId);
    }
}
