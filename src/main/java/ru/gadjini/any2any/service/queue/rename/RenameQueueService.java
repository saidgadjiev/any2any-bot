package ru.gadjini.any2any.service.queue.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.dao.RenameQueueDao;
import ru.gadjini.any2any.domain.RenameQueueItem;

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

    public int createProcessingItem(int userId, RenameState renameState, String newFileName) {
        RenameQueueItem renameQueueItem = new RenameQueueItem();
        renameQueueItem.setUserId(userId);
        renameQueueItem.setFileName(renameState.getFile().getFileName());
        renameQueueItem.setNewFileName(newFileName);
        renameQueueItem.setReplyToMessageId(renameState.getReplyMessageId());
        renameQueueItem.setFileId(renameState.getFile().getFileId());
        renameQueueItem.setMimeType(renameState.getFile().getMimeType());
        renameQueueItem.setStatus(RenameQueueItem.Status.PROCESSING);

        return renameQueueDao.create(renameQueueItem);
    }

    public void setWaiting(int id) {
        renameQueueDao.setWaiting(id);
    }

    public RenameQueueItem peek() {
        return renameQueueDao.poll();
    }

    public void delete(int id) {
        renameQueueDao.delete(id);
    }
}
