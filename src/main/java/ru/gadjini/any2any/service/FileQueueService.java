package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.any2any.dao.FileQueueDao;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.model.TgDocument;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;

@Service
public class FileQueueService {

    private FileQueueDao fileQueueDao;

    @Autowired
    public FileQueueService(FileQueueDao fileQueueDao) {
        this.fileQueueDao = fileQueueDao;
    }

    @Transactional
    public FileQueueItem add(User user, int messageId, TgDocument document, Format targetFormat) {
        FileQueueItem fileQueueItem = new FileQueueItem();
        fileQueueItem.setFileId(document.getFileId());
        fileQueueItem.setSize(document.getFileSize());
        fileQueueItem.setMimeType(document.getMimeType());
        fileQueueItem.setUserId(user.getId());
        fileQueueItem.setMessageId(messageId);
        fileQueueItem.setFileName(document.getFileName());
        fileQueueItem.setTargetFormat(targetFormat);

        fileQueueDao.add(fileQueueItem);
        fileQueueItem.setPlaceInQueue(fileQueueDao.getPlaceInQueue(fileQueueItem.getId()));

        return fileQueueItem;
    }

    public List<FileQueueItem> getItems(int limit) {
        return fileQueueDao.getItems(limit);
    }

    public void delete(int id) {
        fileQueueDao.delete(id);
    }
}
