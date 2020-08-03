package ru.gadjini.any2any.service.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.progress.Lang;

import java.util.Locale;

@Service
public class RenameMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public RenameMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String buildRenamingMessage(RenameStep renameStep, Locale locale, Lang lang) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        switch (renameStep) {
            case DOWNLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale);
            case RENAMING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " " + formatter + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale);
            case UPLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " <b>(" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n";
            default:
                return localisationService.getMessage(MessagesProperties.MESSAGE_DOWNLOADING_STEP, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " " + iconCheck;
        }
    }

}
