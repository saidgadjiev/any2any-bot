package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.dao.FileQueueDao;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;
import java.util.Locale;

@Service
public class FileQueueService {

    private FileQueueDao fileQueueDao;

    private LocalisationService localisationService;

    @Autowired
    public FileQueueService(FileQueueDao fileQueueDao, LocalisationService localisationService) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
    }

    @Transactional
    public FileQueueItem add(User user, ConvertState convertState, Format targetFormat) {
        FileQueueItem fileQueueItem = new FileQueueItem();

        fileQueueItem.setFileId(convertState.getFileId());
        fileQueueItem.setSize(convertState.getFileSize());
        fileQueueItem.setFormat(convertState.getFormat());
        fileQueueItem.setUserId(user.getId());
        fileQueueItem.setMessageId(convertState.getMessageId());
        fileQueueItem.setFileName(convertState.getFileName());
        fileQueueItem.setMimeType(convertState.getMimeType());
        if (StringUtils.isBlank(fileQueueItem.getFileName())) {
            fileQueueItem.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, new Locale(convertState.getUserLanguage())));
        }
        fileQueueItem.setTargetFormat(targetFormat);

        fileQueueDao.add(fileQueueItem);
        fileQueueItem.setPlaceInQueue(fileQueueDao.getPlaceInQueue(fileQueueItem.getId()));

        return fileQueueItem;
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
