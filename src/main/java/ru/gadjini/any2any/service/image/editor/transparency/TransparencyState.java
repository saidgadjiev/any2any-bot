package ru.gadjini.any2any.service.image.editor.transparency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

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
    public TransparencyState(@Qualifier("messageLimits") MessageService messageService,
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
        messageService.editMessageCaption(EditMessageCaption.builder().chatId(String.valueOf(chatId))
                .messageId(state.getMessageId())
                .caption(messageBuilder.getSettingsStr(state))
                .replyMarkup(inlineKeyboardService.getTransparencyKeyboard(new Locale(state.getLanguage()))).build());
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(imageEditorState.getName());
        imageEditorState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }
}
