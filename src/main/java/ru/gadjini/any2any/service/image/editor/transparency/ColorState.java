package ru.gadjini.any2any.service.image.editor.transparency;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ColorState implements State {

    private static final String TAG = "color";

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorState.class);

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private TransparencyState transparencyState;

    private CommandStateService commandStateService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private ThreadPoolTaskExecutor executor;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    private EditMessageBuilder messageBuilder;

    private LocalisationService localisationService;

    @Autowired
    public ColorState(CommandStateService commandStateService, @Qualifier("limits") MessageService messageService,
                      InlineKeyboardService inlineKeyboardService, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                      TempFileService fileService, ImageConvertDevice imageDevice, EditMessageBuilder messageBuilder,
                      LocalisationService localisationService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.executor = executor;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
        this.messageBuilder = messageBuilder;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setTransparencyState(TransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Override
    public Name getName() {
        return Name.COLOR;
    }

    @Override
    public void update(ImageEditorCommand command, long chatId, String queryId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.deleteMessage(chatId, state.getMessageId());
        Locale locale = new Locale(state.getLanguage());
        SendFileResult sendFileResult = messageService.sendDocument(new SendDocument(chatId, state.getFileName(), new File(state.getCurrentFilePath()))
                .setCaption(messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, locale))
                .setReplyMarkup(inlineKeyboardService.getColorsKeyboard(locale, state.canCancel())));
        state.setCurrentFileId(sendFileResult.getFileId());
        state.setMessageId(sendFileResult.getMessageId());
        commandStateService.setState(chatId, command.getHistoryName(), state);

        if (StringUtils.isNotBlank(queryId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.UPDATE_CALLBACK_ANSWER, locale)));
        }
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(transparencyState.getName());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageCaption(new EditMessageCaption(chatId, state.getMessageId(),
                messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, new Locale(state.getLanguage())))
                .setReplyMarkup(inlineKeyboardService.getColorsKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String colorText) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        validateColor(colorText, new Locale(editorState.getLanguage()));

        executor.execute(() -> {
            SmartTempFile tempFile = fileService.getTempFile(TAG, Format.PNG.getExt());

            if (editorState.getMode() == ModeState.Mode.NEGATIVE) {
                String[] transparentColors = getNegativeTransparentColors(colorText);
                imageDevice.negativeTransparent(editorState.getCurrentFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), transparentColors);
            } else {
                imageDevice.positiveTransparent(editorState.getCurrentFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), getPositiveTransparentColor(colorText));
            }
            if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevFilePath()));
                prevFile.smartDelete();
            }
            editorState.setPrevFilePath(editorState.getCurrentFilePath());
            editorState.setPrevFileId(editorState.getCurrentFileId());
            editorState.setCurrentFilePath(tempFile.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            EditMediaResult editMediaResult = messageService.editMessageMedia(new EditMessageMedia(chatId,
                    editorState.getMessageId(), editorState.getFileName(), tempFile.getFile(), messageBuilder.getSettingsStr(editorState) + "\n\n"
                    + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, locale))
                    .setReplyMarkup(inlineKeyboardService.getColorsKeyboard(locale, editorState.canCancel())));
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.TRANSPARENT_COLOR_EDITED_CALLBACK_ANSWER, locale))
                );
            }
        });
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setCurrentFileId(editorState.getPrevFileId());
            editorState.setPrevFilePath(null);
            editorState.setPrevFileId(null);

            messageService.editMessageMedia(new EditMessageMedia(chatId, editorState.getMessageId(), editorState.getCurrentFileId(), messageBuilder.getSettingsStr(editorState))
                    .setReplyMarkup(inlineKeyboardService.getColorsKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))));
        }
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        transparentColor(command, chatId, null, text);
    }

    private String getPositiveTransparentColor(String colorText) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return col.name().toLowerCase();
            }
        }
        return colorText.startsWith("#") ? colorText : '#' + colorText;
    }

    private String[] getNegativeTransparentColors(String colorText) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return col.transparentColors();
            }
        }
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;

        return new String[]{colorText};
    }

    private void validateColor(String colorText, Locale locale) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return;
            }
        }
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;
        if (HEX.matcher(colorText).matches()) {
            return;
        }

        LOGGER.warn("Incorrect color({})", colorText);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_COLOR, locale));
    }
}
