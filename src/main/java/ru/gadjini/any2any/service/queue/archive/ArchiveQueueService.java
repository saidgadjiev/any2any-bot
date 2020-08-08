package ru.gadjini.any2any.service.queue.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.queue.ArchiveQueueDao;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArchiveQueueService {

    private ArchiveQueueDao dao;

    @Autowired
    public ArchiveQueueService(ArchiveQueueDao archiveQueueDao) {
        this.dao = archiveQueueDao;
    }

    public ArchiveQueueItem createProcessingItem(int userId, List<Any2AnyFile> any2AnyFiles, Format format) {
        ArchiveQueueItem archiveQueueItem = new ArchiveQueueItem();
        archiveQueueItem.setStatus(ArchiveQueueItem.Status.PROCESSING);
        archiveQueueItem.setUserId(userId);
        archiveQueueItem.setType(format);
        archiveQueueItem.setFiles(new ArrayList<>());

        for (Any2AnyFile any2AnyFile: any2AnyFiles) {
            TgFile tgFile = new TgFile();
            tgFile.setFileId(any2AnyFile.getFileId());
            tgFile.setFileName(any2AnyFile.getFileName());
            tgFile.setMimeType(any2AnyFile.getMimeType());
            tgFile.setSize(any2AnyFile.getFileSize());
            archiveQueueItem.getFiles().add(tgFile);
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

    public List<Integer> deleteByUserId(int userId) {
        return dao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return dao.exists(jobId);
    }
}
