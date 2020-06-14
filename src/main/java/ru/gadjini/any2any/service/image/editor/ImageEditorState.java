package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.AnswerCallbackContext;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.effects.FiltersState;
import ru.gadjini.any2any.service.image.editor.transparency.TransparencyState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Component
public class ImageEditorState implements State {

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private TransparencyState transparencyState;

    private FiltersState effectsState;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ImageEditorState(CommandStateService commandStateService,
                            LocalisationService localisationService, @Qualifier("limits") MessageService messageService,
                            InlineKeyboardService inlineKeyboardService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setTransparencyState(TransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Autowired
    public void setEffectsState(FiltersState effectsState) {
        this.effectsState = effectsState;
    }

    @Override
    public Name getName() {
        return Name.EDIT;
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

            messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), editorState.getCurrentFileId())
                    .replyKeyboard(inlineKeyboardService.getImageEditKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));
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
    public void go(ImageEditorCommand command, long chatId, Name name) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);

        switch (name) {
            case TRANSPARENCY:
                transparencyState.enter(command, chatId);
                state.setStateName(transparencyState.getName());
                break;
            case FILTERS:
                effectsState.enter(command, chatId);
                state.setStateName(effectsState.getName());
                break;
        }
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getCurrentFileId())
                .replyKeyboard(inlineKeyboardService.getImageEditKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }
}
