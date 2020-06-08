package ru.gadjini.any2any.service.image.editor;

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
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ColorState implements State {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private EditState editState;

    private CommandStateService commandStateService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommonJobExecutor commonJobExecutor;

    private FileService fileService;

    private ImageDevice imageDevice;

    private EditMessageBuilder messageBuilder;

    private LocalisationService localisationService;

    @Autowired
    public ColorState(CommandStateService commandStateService, @Qualifier("limits") MessageService messageService,
                      InlineKeyboardService inlineKeyboardService, CommonJobExecutor commonJobExecutor,
                      FileService fileService, ImageDevice imageDevice, EditMessageBuilder messageBuilder,
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
    public void setEditState(EditState editState) {
        this.editState = editState;
    }

    @Override
    public Name getName() {
        return Name.COLOR;
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        editState.enter(command, callbackQuery.getMessage().getChatId());
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(editState.getName());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editReplyKeyboard(chatId, state.getMessageId(), inlineKeyboardService.getColorsKeyboard(new Locale(state.getLanguage()), state.canCancel()));
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String colorText) {
        commonJobExecutor.addJob(() -> {
            EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
            SmartTempFile tempFile = fileService.getTempFile(editorState.getFileName());

            if (editorState.getMode() == ModeState.Mode.NEGATIVE) {
                String[] transparentColors = getNegativeTransparentColors(colorText);
                imageDevice.negativeTransparent(editorState.getEditFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), transparentColors);
            } else {
                imageDevice.positiveTransparent(editorState.getEditFilePath(), tempFile.getAbsolutePath(), editorState.getInaccuracy(), getPositiveTransparentColor(colorText));
            }
            if (StringUtils.isNotBlank(editorState.getPrevEditFilePath())) {
                try {
                    SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevEditFilePath()), true);
                    prevFile.smartDelete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            editorState.setPrevEditFilePath(editorState.getEditFilePath());
            editorState.setEditFilePath(tempFile.getAbsolutePath());
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
        if (StringUtils.isNotBlank(editorState.getPrevEditFilePath())) {
            String editFilePath = editorState.getEditFilePath();
            editorState.setEditFilePath(editorState.getPrevEditFilePath());
            editorState.setPrevEditFilePath(null);

            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), new File(editorState.getEditFilePath()))
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
