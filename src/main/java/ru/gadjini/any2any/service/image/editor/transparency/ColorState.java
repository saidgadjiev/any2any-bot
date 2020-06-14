package ru.gadjini.any2any.service.image.editor.transparency;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.AnswerCallbackContext;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.EditMessageCaptionContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ColorState implements State {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private TransparencyState transparencyState;

    private CommandStateService commandStateService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommonJobExecutor commonJobExecutor;

    private TempFileService fileService;

    private ImageDevice imageDevice;

    private EditMessageBuilder messageBuilder;

    private LocalisationService localisationService;

    @Autowired
    public ColorState(CommandStateService commandStateService, @Qualifier("limits") MessageService messageService,
                      InlineKeyboardService inlineKeyboardService, CommonJobExecutor commonJobExecutor,
                      TempFileService fileService, ImageDevice imageDevice, EditMessageBuilder messageBuilder,
                      LocalisationService localisationService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commonJobExecutor = commonJobExecutor;
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
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(transparencyState.getName());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageCaption(new EditMessageCaptionContext(chatId, state.getMessageId(),
                messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENT_COLOR_WELCOME, new Locale(state.getLanguage())))
                .replyKeyboard(inlineKeyboardService.getColorsKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String colorText) {
        commonJobExecutor.addJob(() -> {
            EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
            SmartTempFile tempFile = fileService.getTempFile(editorState.getFileName());

            if (editorState.getMode() == ModeState.Mode.NEGATIVE) {
                String[] transparentColors = getNegativeTransparentColors(colorText);
                imageDevice.negativeTransparent(editorState.getCurrentFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), transparentColors);
            } else {
                imageDevice.positiveTransparent(editorState.getCurrentFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), getPositiveTransparentColor(colorText));
            }
            if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
                try {
                    SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevFilePath()), true);
                    prevFile.smartDelete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            editorState.setPrevFilePath(editorState.getCurrentFilePath());
            editorState.setCurrentFilePath(tempFile.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), tempFile.getFile())
                    .caption(messageBuilder.getSettingsStr(editorState))
                    .replyKeyboard(inlineKeyboardService.getColorsKeyboard(locale, editorState.canCancel())));
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackContext(queryId, localisationService.getMessage(MessagesProperties.TRANSPARENT_COLOR_EDITED_CALLBACK_ANSWER, locale))
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
            editorState.setPrevFilePath(null);

            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), new File(editorState.getCurrentFilePath()))
                    .caption(messageBuilder.getSettingsStr(editorState))
                    .replyKeyboard(inlineKeyboardService.getColorsKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            File file = new File(editFilePath);
            try {
                new SmartTempFile(file, true).smartDelete();
            } catch (IOException e) {
                FileUtils.deleteQuietly(file.getParentFile());
            }
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackContext(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))));
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
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;
        if (HEX.matcher(colorText).matches()) {
            return colorText;
        }

        throw new IllegalStateException();
    }

    private String[] getNegativeTransparentColors(String colorText) {
        for (Color col : Color.values()) {
            if (col.name().equalsIgnoreCase(colorText)) {
                return col.transparentColors();
            }
        }
        colorText = colorText.startsWith("#") ? colorText : '#' + colorText;
        if (HEX.matcher(colorText).matches()) {
            return new String[]{colorText};
        }

        throw new IllegalStateException();
    }
}