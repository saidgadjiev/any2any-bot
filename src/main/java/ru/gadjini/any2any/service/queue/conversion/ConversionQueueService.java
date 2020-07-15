package ru.gadjini.any2any.service.queue.conversion;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.dao.queue.ConversionQueueDao;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.model.bot.api.object.User;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TimeCreator;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.List;
import java.util.Locale;

@Service
public class ConversionQueueService {

    private ConversionQueueDao fileQueueDao;

    private LocalisationService localisationService;

    private TimeCreator timeCreator;

    @Autowired
    public ConversionQueueService(ConversionQueueDao fileQueueDao, LocalisationService localisationService, TimeCreator timeCreator) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
        this.timeCreator = timeCreator;
    }

    @Transactional
    public ConversionQueueItem createProcessingItem(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setFileId(convertState.getFileId());
        fileQueueItem.setSize(convertState.getFileSize());
        fileQueueItem.setFormat(convertState.getFormat());
        fileQueueItem.setUserId(user.getId());
        fileQueueItem.setReplyToMessageId(convertState.getMessageId());
        fileQueueItem.setFileName(convertState.getFileName());
        fileQueueItem.setMimeType(convertState.getMimeType());
        fileQueueItem.setStatus(ConversionQueueItem.Status.PROCESSING);
        if (StringUtils.isBlank(fileQueueItem.getFileName())) {
            fileQueueItem.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, new Locale(convertState.getUserLanguage())));
        }
        fileQueueItem.setTargetFormat(targetFormat);

        fileQueueItem.setLastRunAt(timeCreator.now());
        fileQueueItem.setStatedAt(timeCreator.now());
        fileQueueDao.create(fileQueueItem);
        fileQueueItem.setPlaceInQueue(fileQueueDao.getPlaceInQueue(fileQueueItem.getId()));

        return fileQueueItem;
    }

    public ConversionQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<ConversionQueueItem> poll = fileQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<ConversionQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return fileQueueDao.poll(weight, limit);
    }

    public void setWaiting(int id) {
        fileQueueDao.setWaiting(id);
    }

    public void resetProcessing() {
        fileQueueDao.resetProcessing();
    }

    public ConversionQueueItem delete(int id) {
        return fileQueueDao.delete(id);
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

    public List<ConversionQueueItem> getActiveItems(int userId) {
        return fileQueueDao.getActiveQueries(userId);
    }

    public ConversionQueueItem getItem(int id) {
        return fileQueueDao.getById(id);
    }

    public ConversionQueueItem poll(int id) {
        return fileQueueDao.poll(id);
    }
}
