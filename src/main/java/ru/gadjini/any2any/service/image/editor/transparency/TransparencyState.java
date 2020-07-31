package ru.gadjini.any2any.service.image.editor.transparency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class TransparencyState implements State {

    private ColorState colorState;

    private InaccuracyState inaccuracyState;

    private ModeState modeState;

    private ImageEditorState imageEditorState;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private EditMessageBuilder messageBuilder;

    @Autowired
    public TransparencyState(@Qualifier("limits") MessageService messageService,
                             InlineKeyboardService inlineKeyboardService, CommandStateService commandStateService, EditMessageBuilder messageBuilder) {
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.messageBuilder = messageBuilder;
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

    @Autowired
    public void setImageEditorState(ImageEditorState imageEditorState) {
        this.imageEditorState = imageEditorState;
    }

    @Override
    public Name getName() {
        return Name.TRANSPARENCY;
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, String queryId, Name name) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);

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
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.editMessageCaption(new EditMessageCaption(chatId, state.getMessageId(),
                messageBuilder.getSettingsStr(state))
                .setReplyMarkup(inlineKeyboardService.getTransparencyKeyboard(new Locale(state.getLanguage()))));
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(imageEditorState.getName());
        imageEditorState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }
}
