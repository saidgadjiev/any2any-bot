package ru.gadjini.any2any.service.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.update.UpdateQueryStatusCommandMessageProvider;

import java.util.Locale;

@Service
public class ArchiveMessageBuilder implements UpdateQueryStatusCommandMessageProvider {

    private LocalisationService localisationService;

    @Autowired
    public ArchiveMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    public String getUpdateStatusMessage(QueueItem queueItem, Locale locale) {
        return getWaitingMessage(queueItem, locale);
    }

    public String getWaitingMessage(QueueItem queueItem, Locale locale) {
        return buildArchiveProcessMessage((ArchiveQueueItem) queueItem, ArchiveStep.WAITING, locale);
    }

    public String buildArchiveProgressMessage(ArchiveQueueItem queueItem, int count, int current, ArchiveStep archiveStep, Locale locale) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale) + "\n\n" +
                localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_DOWNLOADING, new Object[]{current - 1, count}, locale) + "\n" +
                buildArchiveProcessMessage(archiveStep, locale) + "\n\n" +
                localisationService.getMessage(MessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale);
    }

    public String buildArchiveProcessMessage(ArchiveQueueItem queueItem, ArchiveStep archiveStep, Locale locale) {
        return localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale) + "\n\n" +
                buildArchiveProcessMessage(archiveStep, locale) + "\n\n" +
                localisationService.getMessage(MessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale);
    }

    private String buildArchiveProcessMessage(ArchiveStep archiveStep, Locale locale) {
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);

        switch (archiveStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + " ...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + " ...</b>";
            case ARCHIVE_CREATION:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + " ...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }
}
