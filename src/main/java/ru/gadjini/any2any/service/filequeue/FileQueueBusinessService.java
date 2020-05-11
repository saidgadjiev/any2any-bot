package ru.gadjini.any2any.service.filequeue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.FileQueueDao;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.event.QueueItemCanceled;

import java.util.List;

@Service
public class FileQueueBusinessService {

    private ApplicationEventPublisher eventPublisher;

    private FileQueueDao fileQueueDao;

    @Autowired
    public FileQueueBusinessService(ApplicationEventPublisher eventPublisher, FileQueueDao fileQueueDao) {
        this.eventPublisher = eventPublisher;
        this.fileQueueDao = fileQueueDao;
    }

    public List<FileQueueItem> takeItems(int limit) {
        return fileQueueDao.takeItems(limit);
    }

    public void resetProcessing() {
        fileQueueDao.resetProcessing();
    }

    public void exception(int id, Exception ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.updateException(id, FileQueueItem.Status.EXCEPTION.getCode(), exception);
    }

    public void converterNotFound(int id) {
        fileQueueDao.updateException(id, FileQueueItem.Status.CANDIDATE_NOT_FOUND.getCode(), "Converter not found");
    }

    public void complete(int id) {
        fileQueueDao.updateCompletedAt(id, FileQueueItem.Status.COMPLETED.getCode());
    }

    public void cancel(int id) {
        fileQueueDao.delete(id);
        eventPublisher.publishEvent(new QueueItemCanceled(id));
    }
}
