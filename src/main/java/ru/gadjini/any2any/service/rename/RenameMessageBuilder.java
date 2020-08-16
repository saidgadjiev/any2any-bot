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
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + " (" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case RENAMING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " (" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " (" + formatter + percentage + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n";
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

}
