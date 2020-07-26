package ru.gadjini.any2any.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.command.CommandParser;
import ru.gadjini.any2any.service.image.editor.State;

import java.util.Locale;
import java.util.Objects;

@Service
public class ButtonFactory {

    private LocalisationService localisationService;

    @Autowired
    public ButtonFactory(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public InlineKeyboardButton toNextPage(String delegate, int prevLimit, int offset, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.MESSAGE_NEXT_PAGE, locale));
        button.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.OFFSET.getKey(), offset)
                        .add(Arg.PREV_LIMIT.getKey(), prevLimit)
                        .add(Arg.PAGINATION.getKey(), true)
                        .add(Arg.CALLBACK_DELEGATE.getKey(), delegate)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton toPrevPage(String delegate, int prevLimit, int offset, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.MESSAGE_PREV_PAGE, locale));
        button.setCallbackData(CommandNames.CALLBACK_DELEGATE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.OFFSET.getKey(), offset)
                        .add(Arg.PREV_LIMIT.getKey(), prevLimit)
                        .add(Arg.PAGINATION.getKey(), true)
                        .add(Arg.CALLBACK_DELEGATE.getKey(), delegate)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton resizeButton(Locale locale) {
        return delegateButton(MessagesProperties.RESIZE_IMAGE_COMMAND_DESCRIPTION, CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.RESIZE.name()), locale);
    }

    public InlineKeyboardButton blackAndWhiteFilterButton(Locale locale) {
        return delegateButton(MessagesProperties.BLACK_WHITE_FILTER_COMMAND_DESCRIPTION, CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.BLACK_AND_WHITE.name()), locale);
    }

    public InlineKeyboardButton sketchFilterButton(Locale locale) {
        return delegateButton(MessagesProperties.SKETCH_FILTER_COMMAND_DESCRIPTION, CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.SKETCH.name()), locale);
    }

    public InlineKeyboardButton cancelArchiveCreatingQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.CANCEL_ARCHIVE_QUERY + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton cancelRenameQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.CANCEL_RENAME_QUERY + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton cancelArchiveFiles(Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.CANCEL_ARCHIVE_FILES + CommandParser.COMMAND_NAME_SEPARATOR);

        return button;
    }

    public InlineKeyboardButton cancelUnzipQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.CANCEL_UNZIP_QUERY + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton cancelExtractFileQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.CANCEL_EXTRACT_FILE + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }

    public InlineKeyboardButton updateButton(Locale locale) {
        return delegateButton(MessagesProperties.UPDATE_COMMAND_DESCRIPTION, CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.UPDATE_EDITED_IMAGE.getKey(), "u"), locale);
    }

    public InlineKeyboardButton negativeButton(Locale locale) {
        return delegateButton(MessagesProperties.NEGATIVE_FILTER_COMMAND_DESCRIPTION, CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                new RequestParams().add(Arg.IMAGE_FILTER.getKey(), State.Filter.NEGATIVE.name()), locale);
    }

    public InlineKeyboardButton transparencyButton(Locale locale) {
        return delegateButton(MessagesProperties.IMAGE_TRANSPARENCY_COMMAND_DESCRIPTION,
                CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.TRANSPARENCY.name()), locale);
    }

    public InlineKeyboardButton filtersButton(Locale locale) {
        return delegateButton(MessagesProperties.IMAGE_FILTERS_COMMAND_DESCRIPTION,
                CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.FILTERS.name()), locale);
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

    public InlineKeyboardButton extractAllButton(int unzipJobId, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.EXTRACT_ALL_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.EXTRACT_ALL + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.JOB_ID.getKey(), unzipJobId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton extractFileButton(String name, int id, int unzipJobId) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(name);
        inlineKeyboardButton.setCallbackData(CommandNames.EXTRACT_FILE_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.EXTRACT_FILE_ID.getKey(), id)
                        .add(Arg.JOB_ID.getKey(), unzipJobId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton queryItemDetails(String name, int queryItemId) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(name);
        inlineKeyboardButton.setCallbackData(CommandNames.QUERY_ITEM_DETAILS_COMMAND + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.QUEUE_ITEM_ID.getKey(), queryItemId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton cancelQueryItem(int queryItemId, String actionFrom, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_QUERY_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.CANCEL_QUERY_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.QUEUE_ITEM_ID.getKey(), queryItemId)
                        .add(Arg.ACTION_FROM.getKey(), actionFrom)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton report(int queryItemId, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.REPORT_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(CommandNames.REPORT_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.QUEUE_ITEM_ID.getKey(), queryItemId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton goBackCallbackButton(String prevHistoryName, Locale locale) {
        Objects.requireNonNull(prevHistoryName);
        RequestParams requestParams = new RequestParams();
        requestParams.add(Arg.PREV_HISTORY_NAME.getKey(), prevHistoryName);

        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(CommandNames.GO_BACK_CALLBACK_COMMAND_NAME + CommandParser.COMMAND_NAME_SEPARATOR + requestParams.serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }
}
