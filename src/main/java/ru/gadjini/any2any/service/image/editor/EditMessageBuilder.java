package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

import java.util.Locale;

@Service
public class EditMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public EditMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getSettingsStr(EditorState editorState) {
        Locale locale = new Locale(editorState.getLanguage());
        return localisationService.getMessage(
                MessagesProperties.MESSAGE_EDITOR_SETTINGS,
                new Object[]{
                        editorState.getMode() == ModeState.Mode.NEGATIVE ? localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_NEGATIVE_MODE, locale)
                                : localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_POSITIVE_MODE, locale),
                        editorState.getInaccuracy() + "%"
                },
                locale
        );
    }

}
