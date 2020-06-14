package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.effects.EffectsState;
import ru.gadjini.any2any.service.image.editor.transparency.TransparencyState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class ImageEditorState implements State {

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private TransparencyState transparencyState;

    private EffectsState effectsState;

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
    public void setEffectsState(EffectsState effectsState) {
        this.effectsState = effectsState;
    }

    @Override
    public Name getName() {
        return Name.EDIT;
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, Name name) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);

        switch (name) {
            case TRANSPARENCY:
                transparencyState.enter(command, chatId);
                state.setStateName(transparencyState.getName());
                break;
            case EFFECTS:
                effectsState.enter(command, chatId);
                state.setStateName(effectsState.getName());
                break;
        }
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getFileId())
                .replyKeyboard(inlineKeyboardService.getImageEditKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }
}
