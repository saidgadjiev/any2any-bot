package ru.gadjini.any2any.service.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.ArchiveQueueDao;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArchiveQueueService {

    private ArchiveQueueDao dao;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public ArchiveQueueService(ArchiveQueueDao archiveQueueDao, FileLimitProperties fileLimitProperties) {
        this.dao = archiveQueueDao;
        this.fileLimitProperties = fileLimitProperties;
    }

    public ArchiveQueueItem createItem(int userId, List<MessageMedia> any2AnyFiles, Format format) {
        ArchiveQueueItem archiveQueueItem = new ArchiveQueueItem();
        archiveQueueItem.setStatus(ArchiveQueueItem.Status.WAITING);
        archiveQueueItem.setUserId(userId);
        archiveQueueItem.setType(format);
        archiveQueueItem.setFiles(new ArrayList<>());

        for (MessageMedia any2AnyFile : any2AnyFiles) {
            archiveQueueItem.getFiles().add(any2AnyFile.toTgFile());
        }
        archiveQueueItem.setTotalFileSize(archiveQueueItem.getFiles().stream().map(TgFile::getSize).mapToLong(i -> i).sum());

        int id = dao.create(archiveQueueItem);
        archiveQueueItem.setId(id);
        archiveQueueItem.setQueuePosition(dao.getQueuePosition(archiveQueueItem.getId(), archiveQueueItem.getTotalFileSize() > fileLimitProperties.getLightFileMaxWeight()
                ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));

        return archiveQueueItem;
    }
}
