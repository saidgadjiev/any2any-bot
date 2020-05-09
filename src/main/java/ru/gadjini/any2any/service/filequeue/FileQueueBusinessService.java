package ru.gadjini.any2any.service.filequeue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.FileQueueDao;
import ru.gadjini.any2any.domain.FileQueueItem;

import java.util.List;

@Service
public class FileQueueBusinessService {

    private FileQueueDao fileQueueDao;

    @Autowired
    public FileQueueBusinessService(FileQueueDao fileQueueDao) {
        this.fileQueueDao = fileQueueDao;
    }

    public List<FileQueueItem> takeItems(int limit) {
        return fileQueueDao.takeItems(limit);
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
}
