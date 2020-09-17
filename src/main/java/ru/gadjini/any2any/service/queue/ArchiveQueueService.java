package ru.gadjini.any2any.service.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.ArchiveQueueDao;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArchiveQueueService {

    private ArchiveQueueDao dao;

    @Autowired
    public ArchiveQueueService(ArchiveQueueDao archiveQueueDao) {
        this.dao = archiveQueueDao;
    }

    public ArchiveQueueItem createProcessingItem(int userId, List<MessageMedia> any2AnyFiles, Format format) {
        ArchiveQueueItem archiveQueueItem = new ArchiveQueueItem();
        archiveQueueItem.setStatus(ArchiveQueueItem.Status.PROCESSING);
        archiveQueueItem.setUserId(userId);
        archiveQueueItem.setType(format);
        archiveQueueItem.setFiles(new ArrayList<>());

        for (MessageMedia any2AnyFile: any2AnyFiles) {
            archiveQueueItem.getFiles().add(any2AnyFile.toTgFile());
        }
        archiveQueueItem.setTotalFileSize(archiveQueueItem.getFiles().stream().map(TgFile::getSize).mapToLong(i -> i).sum());

        int id = dao.create(archiveQueueItem);
        archiveQueueItem.setId(id);

        return archiveQueueItem;
    }

    public void resetProcessing() {
        dao.resetProcessing();
    }

    public void setWaiting(int id) {
        dao.setWaiting(id);
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        dao.setProgressMessageId(id, progressMessageId);
    }

    public ArchiveQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<ArchiveQueueItem> poll = dao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<ArchiveQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return dao.poll(weight, limit);
    }

    public ArchiveQueueItem deleteWithReturning(int id) {
        return dao.deleteWithReturning(id);
    }

    public void delete(int id) {
        dao.delete(id);
    }

    public ArchiveQueueItem deleteByUserId(int userId) {
        return dao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return dao.exists(jobId);
    }
}
