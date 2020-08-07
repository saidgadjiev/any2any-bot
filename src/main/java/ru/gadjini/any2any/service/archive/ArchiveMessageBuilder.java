package ru.gadjini.any2any.service.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.unzip.ExtractFileStep;

import java.util.Locale;

@Service
public class ArchiveMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public ArchiveMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String buildArchiveProgressMessage(int count, int current, ArchiveStep archiveStep, Lang lang, Locale locale) {
        String message = localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_DOWNLOADING, new Object[]{count, current}, locale);

        return message + "\n" + buildArchiveProcessMessage(archiveStep, lang, locale);
    }

    public String buildArchiveProcessMessage(ArchiveStep archiveStep, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);

        switch (archiveStep) {
            case DOWNLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_CREATION_STEP, locale) + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale);
            case UPLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_CREATION_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n";
            default:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_CREATION_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " " + iconCheck;
        }
    }
}
