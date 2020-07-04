package ru.gadjini.any2any.service.queue.conversion;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.ConversionQueueDao;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.event.QueueItemCanceled;

import java.util.List;

@Service
public class ConversionQueueBusinessService {

    private ApplicationEventPublisher eventPublisher;

    private ConversionQueueDao fileQueueDao;

    @Autowired
    public ConversionQueueBusinessService(ApplicationEventPublisher eventPublisher, ConversionQueueDao fileQueueDao) {
        this.eventPublisher = eventPublisher;
        this.fileQueueDao = fileQueueDao;
    }

    public List<ConversionQueueItem> takeItems(int limit) {
        return fileQueueDao.takeItems(limit);
    }

    public ConversionQueueItem takeItem() {
        List<ConversionQueueItem> items = fileQueueDao.takeItems(1);

        return items.isEmpty() ? null : items.iterator().next();
    }

    public void resetProcessing() {
        fileQueueDao.resetProcessing();
    }

    public void exception(int id, Exception ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        fileQueueDao.updateException(id, ConversionQueueItem.Status.EXCEPTION.getCode(), exception);
    }

    public void completeWithException(int id, String msg) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.COMPLETED.getCode(), msg);
    }

    public void converterNotFound(int id) {
        fileQueueDao.updateException(id, ConversionQueueItem.Status.CANDIDATE_NOT_FOUND.getCode(), "Converter not found");
    }

    public void complete(int id) {
        fileQueueDao.updateCompletedAt(id, ConversionQueueItem.Status.COMPLETED.getCode());
    }

    public void cancel(int id) {
        fileQueueDao.delete(id);
        eventPublisher.publishEvent(new QueueItemCanceled(id));
    }

    public void setWaiting(int id) {
        fileQueueDao.setWaiting(id);
    }
}
