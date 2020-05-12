package ru.gadjini.any2any.service.filequeue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.Time2TextService;
import ru.gadjini.any2any.service.TimeCreator;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class FileQueueMessageBuilder {

    private LocalisationService localisationService;

    private TimeCreator timeCreator;

    private Time2TextService time2TextService;

    @Autowired
    public FileQueueMessageBuilder(LocalisationService localisationService, TimeCreator timeCreator, Time2TextService time2TextService) {
        this.localisationService = localisationService;
        this.timeCreator = timeCreator;
        this.time2TextService = time2TextService;
    }

    public String getItems(List<FileQueueItem> queueItems, Locale locale) {
        if (queueItems.isEmpty()) {
            return localisationService.getMessage(MessagesProperties.MESSAGE_QUERIES_EMPTY, locale);
        }
        StringBuilder message = new StringBuilder();
        int i = 1;
        for (FileQueueItem fileQueueItem : queueItems) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message
                    .append(i++).append(") ").append(getFileName(fileQueueItem)).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_TARGET_FORMAT, new Object[]{fileQueueItem.getTargetFormat().name()}, locale))
                    .append("\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_STATUS, new Object[]{getStatus(fileQueueItem, locale)}, locale));
        }

        return message.toString();
    }

    public String queryItemNotFound(Locale locale) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, locale);
    }

    public String getItem(FileQueueItem fileQueueItem, Locale locale) {
        StringBuilder message = new StringBuilder();
        message
                .append(getFileName(fileQueueItem)).append(" ")
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_TARGET_FORMAT, new Object[]{fileQueueItem.getTargetFormat().name()}, locale))
                .append("\n")
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_STATUS, new Object[]{getStatus(fileQueueItem, locale)}, locale));

        return message.toString();
    }

    private String getFileName(FileQueueItem fileQueueItem) {
        switch (fileQueueItem.getFormat()) {
            case TEXT:
            case URL:
                if (fileQueueItem.getFileId().length() > 11) {
                    return fileQueueItem.getFileId().substring(0, 11) + "...";
                } else {
                    return fileQueueItem.getFileId();
                }
            default:
                return fileQueueItem.getFileName();
        }
    }

    private String getStatus(FileQueueItem fileQueueItem, Locale locale) {
        switch (fileQueueItem.getStatus()) {
            case WAITING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_WAITING, new Object[]{fileQueueItem.getPlaceInQueue()}, locale);
            case PROCESSING:
                Duration between = Duration.between(fileQueueItem.getLastRunAt(), timeCreator.now());
                String time = time2TextService.time(between, locale);
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_PROCESSING, new Object[]{time}, locale);
            case EXCEPTION:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_EXCEPTION, locale);
        }

        return "";
    }
}
