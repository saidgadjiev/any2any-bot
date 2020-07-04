package ru.gadjini.any2any.service.queue.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.ArchiveQueueDao;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.model.Any2AnyFile;
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
            archiveQueueItem.getFiles().add(tgFile);
        }

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

    public ArchiveQueueItem peek() {
        return dao.poll();
    }

    public void delete(int id) {
        dao.delete(id);
    }
}
