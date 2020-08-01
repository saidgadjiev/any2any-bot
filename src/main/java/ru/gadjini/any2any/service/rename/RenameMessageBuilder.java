package ru.gadjini.any2any.service.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.LocalisationService;

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
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        switch (renameStep) {
            case DOWNLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_ONE, locale) + " <b>(" + formatter + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_THREE, locale);
            case RENAMING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_ONE, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " <b>(" + formatter + ")...</b>\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_THREE, locale);
            case UPLOADING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_ONE, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_THREE, locale) + " <b>(" + formatter + ")...</b>";
            default:
                return localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_ONE, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " " + iconCheck + "\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_THREE, locale) + " " + iconCheck;
        }
    }

    public enum Lang {
        JAVA,

        PYTHON
    }
}
