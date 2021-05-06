package ru.gadjini.any2any.service.image.editor.transparency;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.EditMediaResult;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@SuppressWarnings("CPD-START")
public class ColorState implements State {

    public static final String TAG = "color";

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorState.class);

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private TransparencyState transparencyState;

    private CommandStateService commandStateService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private InlineKeyboardService inlineKeyboardService;

    private ThreadPoolTaskExecutor executor;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    private EditMessageBuilder messageBuilder;

    private LocalisationService localisationService;

    @Autowired
    public ColorState(CommandStateService commandStateService, @TgMessageLimitsControl MessageService messageService,
                      @Qualifier("mediaLimits") MediaMessageService mediaMessageService, InlineKeyboardService inlineKeyboardService,
                      @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                      TempFileService fileService, ImageConvertDevice imageDevice, EditMessageBuilder messageBuilder,
                      LocalisationService localisationService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
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
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.deleteMessage(chatId, state.getMessageId());
        Locale locale = new Locale(state.getLanguage());

        SendFileResult sendFileResult = mediaMessageService.sendDocument(SendDocument.builder().chatId(String.valueOf(chatId))
                .document(new InputFile(new File(state.getCurrentFilePath()), state.getFileName()))
                .caption(messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, locale))
                .replyMarkup(inlineKeyboardService.getColorsKeyboard(locale, state.canCancel())).build());
        state.setCurrentFileId(sendFileResult.getFileId());
        state.setMessageId(sendFileResult.getMessageId());
        commandStateService.setState(chatId, command.getHistoryName(), state);

        if (StringUtils.isNotBlank(queryId)) {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.UPDATE_CALLBACK_ANSWER, locale)).build());
        }
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(transparencyState.getName());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.editMessageCaption(EditMessageCaption.builder().chatId(String.valueOf(chatId))
                .messageId(state.getMessageId())
                .caption(messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, new Locale(state.getLanguage())))
                .replyMarkup(inlineKeyboardService.getColorsKeyboard(new Locale(state.getLanguage()), state.canCancel())).build());
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String colorText) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        validateColor(colorText, new Locale(editorState.getLanguage()));

        executor.execute(() -> {
            SmartTempFile tempFile = fileService.getTempFile(FileTarget.TEMP, chatId, editorState.getCurrentFileId(), TAG, Format.PNG.getExt());

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
            InputMediaDocument inputMediaDocument = new InputMediaDocument();
            inputMediaDocument.setMedia(tempFile.getFile(), editorState.getFileName());
            inputMediaDocument.setCaption(
                    messageBuilder.getSettingsStr(editorState) + "\n\n"
                            + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, locale));
            EditMediaResult editMediaResult = mediaMessageService.editMessageMedia(EditMessageMedia.builder().chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(inputMediaDocument)
                    .replyMarkup(inlineKeyboardService.getColorsKeyboard(locale, editorState.canCancel())).build());
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        AnswerCallbackQuery.builder().callbackQueryId(queryId)
                                .text(localisationService.getMessage(MessagesProperties.TRANSPARENT_COLOR_EDITED_CALLBACK_ANSWER, locale)).build()
                );
            }
        });
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setCurrentFileId(editorState.getPrevFileId());
            editorState.setPrevFilePath(null);
            editorState.setPrevFileId(null);

            mediaMessageService.editMessageMedia(EditMessageMedia.builder().chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(InputMediaDocument.builder().media(editorState.getCurrentFileId()).caption(messageBuilder.getSettingsStr(editorState)).build())
                    .replyMarkup(inlineKeyboardService.getColorsKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())).build());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))).build());
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
