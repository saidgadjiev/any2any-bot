package ru.gadjini.any2any.service.image.editor.effects;

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
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Component
public class EffectsState implements State {

    private CommandStateService commandStateService;

    private ImageEditorState imageEditorState;

    private ImageDevice imageDevice;

    private TempFileService tempFileService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private CommonJobExecutor commonJobExecutor;

    @Autowired
    public EffectsState(CommandStateService commandStateService, ImageDevice imageDevice,
                        TempFileService tempFileService, @Qualifier("limits") MessageService messageService,
                        InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                        CommonJobExecutor commonJobExecutor) {
        this.commandStateService = commandStateService;
        this.imageDevice = imageDevice;
        this.tempFileService = tempFileService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
        this.commonJobExecutor = commonJobExecutor;
    }

    @Autowired
    public void setImageEditorState(ImageEditorState imageEditorState) {
        this.imageEditorState = imageEditorState;
    }

    @Override
    public Name getName() {
        return Name.EFFECTS;
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setPrevFilePath(null);

            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), new File(editorState.getCurrentFilePath()))
                    .replyKeyboard(inlineKeyboardService.getImageEffectsKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));
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
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getFileId())
                .replyKeyboard(inlineKeyboardService.getImageEffectsKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }

    @Override
    public void applyEffect(ImageEditorCommand command, long chatId, String queryId, Effect effect) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        commonJobExecutor.addJob(() -> {
            SmartTempFile result = tempFileService.getTempFile(editorState.getFileName());
            switch (effect) {
                case SKETCH:
                    imageDevice.applySketchEffect(editorState.getCurrentFilePath(), result.getAbsolutePath());
                    break;
                case BLACK_AND_WHITE:
                    imageDevice.applyBlackAndWhiteEffect(editorState.getCurrentFilePath(), result.getAbsolutePath());
                    break;
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
            editorState.setCurrentFilePath(result.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), result.getFile())
                    .replyKeyboard(inlineKeyboardService.getImageEffectsKeyboard(locale, editorState.canCancel())));
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackContext(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_EFFECT_APPLIED_ANSWER, locale))
                );
            }
        });
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(imageEditorState.getName());
        imageEditorState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }
}
