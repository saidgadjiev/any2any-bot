package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class EditState implements State {

    private ColorState colorState;

    private InaccuracyState inaccuracyState;

    private ModeState modeState;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    @Autowired
    public EditState(@Qualifier("limits") MessageService messageService,
                     InlineKeyboardService inlineKeyboardService, CommandStateService commandStateService) {
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setColorState(ColorState colorState) {
        this.colorState = colorState;
    }

    @Autowired
    public void setInaccuracyState(InaccuracyState inaccuracyState) {
        this.inaccuracyState = inaccuracyState;
    }

    @Autowired
    public void setModeState(ModeState modeState) {
        this.modeState = modeState;
    }

    @Override
    public Name getName() {
        return Name.EDIT;
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, Name name) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);

        switch (name) {
            case COLOR:
                colorState.enter(command, chatId);
                state.setStateName(colorState.getName());
                break;
            case MODE:
                modeState.enter(command, chatId);
                state.setStateName(modeState.getName());
                break;
            case INACCURACY:
                inaccuracyState.enter(command, chatId);
                state.setStateName(inaccuracyState.getName());
                break;
        }
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editReplyKeyboard(chatId, state.getMessageId(), inlineKeyboardService.getEditImageTransparentKeyboard(new Locale(state.getLanguage())));
    }
}
