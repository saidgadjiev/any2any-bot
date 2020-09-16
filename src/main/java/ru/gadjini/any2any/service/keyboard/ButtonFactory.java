package ru.gadjini.any2any.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.Locale;

@Service
public class ButtonFactory {

    private LocalisationService localisationService;

    @Autowired
    public ButtonFactory(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public InlineKeyboardButton resizeButton(Locale locale) {
        return delegateButton(MessagesProperties.RESIZE_IMAGE_COMMAND_DESCRIPTION, FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.RESIZE.name()), locale);
    }

    public InlineKeyboardButton blackAndWhiteFilterButton(Locale locale) {
        return delegateButton(MessagesProperties.BLACK_WHITE_FILTER_COMMAND_DESCRIPTION, FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.BLACK_AND_WHITE.name()), locale);
    }

    public InlineKeyboardButton sketchFilterButton(Locale locale) {
        return delegateButton(MessagesProperties.SKETCH_FILTER_COMMAND_DESCRIPTION, FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.SKETCH.name()), locale);
    }

    public InlineKeyboardButton cancelArchiveCreatingQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(FileUtilsCommandNames.CANCEL_ARCHIVE_QUERY + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton cancelArchiveFiles(Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(FileUtilsCommandNames.CANCEL_ARCHIVE_FILES + CommandParser.COMMAND_NAME_SEPARATOR);

        return button;
    }

    public InlineKeyboardButton updateButton(Locale locale) {
        return delegateButton(MessagesProperties.UPDATE_COMMAND_DESCRIPTION, FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.UPDATE_EDITED_IMAGE.getKey(), "u"), locale);
    }

    public InlineKeyboardButton negativeButton(Locale locale) {
        return delegateButton(MessagesProperties.NEGATIVE_FILTER_COMMAND_DESCRIPTION, FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.NEGATIVE.name()), locale);
    }

    public InlineKeyboardButton transparencyButton(Locale locale) {
        return delegateButton(MessagesProperties.IMAGE_TRANSPARENCY_COMMAND_DESCRIPTION,
                FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.TRANSPARENCY.name()), locale);
    }

    public InlineKeyboardButton filtersButton(Locale locale) {
        return delegateButton(MessagesProperties.IMAGE_FILTERS_COMMAND_DESCRIPTION,
                FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.FILTERS.name()), locale);
    }

    public InlineKeyboardButton moreColorsButton(Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.MORE_COLORS_COMMAND_DESCRIPTION, locale));
        button.setUrl(localisationService.getMessage(MessagesProperties.COLOR_PICKER, locale));

        return button;
    }

    public InlineKeyboardButton imageColorButton(Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.IMAGE_COLOR_COMMAND_DESCRIPTION, locale));
        button.setUrl(localisationService.getMessage(MessagesProperties.IMAGE_COLOR_PICKER, locale));

        return button;
    }

    public InlineKeyboardButton delegateButton(String name, String delegate, RequestParams requestParams) {
        requestParams.add(Arg.CALLBACK_DELEGATE.getKey(), delegate);

        InlineKeyboardButton button = new InlineKeyboardButton(name);
        button.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                requestParams.serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton cancelButton(Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CANCEL_COMMAND_NAME);

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton delegateButton(String nameCode, String delegate, RequestParams requestParams, Locale locale) {
        return delegateButton(localisationService.getMessage(nameCode, locale), delegate, requestParams);
    }
}
