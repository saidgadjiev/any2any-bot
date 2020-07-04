package ru.gadjini.any2any.service.queue.conversion;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.dao.ConversionQueueDao;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.model.bot.api.object.User;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;
import java.util.Locale;

@Service
public class ConversionQueueService {

    private ConversionQueueDao fileQueueDao;

    private LocalisationService localisationService;

    @Autowired
    public ConversionQueueService(ConversionQueueDao fileQueueDao, LocalisationService localisationService) {
        this.fileQueueDao = fileQueueDao;
        this.localisationService = localisationService;
    }

    @Transactional
    public ConversionQueueItem add(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setFileId(convertState.getFileId());
        fileQueueItem.setSize(convertState.getFileSize());
        fileQueueItem.setFormat(convertState.getFormat());
        fileQueueItem.setUserId(user.getId());
        fileQueueItem.setReplyToMessageId(convertState.getMessageId());
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

    public List<ConversionQueueItem> getActiveItems(int userId) {
        return fileQueueDao.getActiveQueries(userId);
    }

    public ConversionQueueItem getItem(int id) {
        return fileQueueDao.getById(id);
    }
}
