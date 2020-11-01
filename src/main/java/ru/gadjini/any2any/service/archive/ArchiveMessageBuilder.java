package ru.gadjini.any2any.service.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;

import java.util.Locale;

@Service
public class ArchiveMessageBuilder {

    private LocalisationService localisationService;

    private ProgressManager progressManager;

    @Autowired
    public ArchiveMessageBuilder(LocalisationService localisationService, ProgressManager progressManager) {
        this.localisationService = localisationService;
        this.progressManager = progressManager;
    }

    public String buildArchiveProgressMessage(ArchiveQueueItem queueItem, int count, int current, ArchiveStep archiveStep, long fileSize, Lang lang, Locale locale) {
        StringBuilder message = new StringBuilder();
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale)).append("\n\n");
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_DOWNLOADING, new Object[]{current - 1, count}, locale)).append("\n");
        message.append(buildArchiveProcessMessage(archiveStep, fileSize, lang, locale));

        return message.toString();
    }

    public String buildArchiveProcessMessage(ArchiveQueueItem queueItem, ArchiveStep archiveStep, long fileSize, Lang lang, Locale locale) {
        StringBuilder message = new StringBuilder();
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale)).append("\n\n");
        message.append(buildArchiveProcessMessage(archiveStep, fileSize, lang, locale));

        return message.toString();
    }

    private String buildArchiveProcessMessage(ArchiveStep archiveStep, long fileSize, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        boolean progress = isShowingProgress(fileSize, archiveStep);
        String percentageFormatter = progress ? "(" + formatter + percentage + ")..." : "...";

        switch (archiveStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + " " + percentageFormatter + "</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n" : "") +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + " " + percentageFormatter + "</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>" : "");
            case ARCHIVE_CREATION:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + " (" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b>";
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.DOWNLOADING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.ARCHIVE_CREATION_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

    private boolean isShowingProgress(long fileSize, ArchiveStep archiveStep) {
        if (archiveStep == ArchiveStep.DOWNLOADING) {
            return progressManager.isShowingDownloadingProgress(fileSize);
        } else if (archiveStep == ArchiveStep.UPLOADING) {
            return progressManager.isShowingUploadingProgress(fileSize);
        }

        return false;
    }
}
