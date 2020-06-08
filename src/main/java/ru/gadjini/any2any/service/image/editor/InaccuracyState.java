package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.model.EditMessageCaptionContext;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class InaccuracyState implements State {

    private EditState editState;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private EditMessageBuilder messageBuilder;

    @Autowired
    public InaccuracyState(CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService,
                           @Qualifier("limits") MessageService messageService, EditMessageBuilder messageBuilder) {
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
    }

    @Autowired
    public void setEditState(EditState editState) {
        this.editState = editState;
    }

    @Override
    public Name getName() {
        return Name.INACCURACY;
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
        messageService.editReplyKeyboard(chatId, state.getMessageId(), inlineKeyboardService.getInaccuracyKeyboard(new Locale(state.getLanguage())));
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String inaccuracy) {
        double v = Double.parseDouble(inaccuracy);
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        state.setInaccuracy(String.valueOf(v));
        messageService.editMessageCaption(
                new EditMessageCaptionContext(chatId, state.getMessageId(), messageBuilder.getSettingsStr(state))
                        .replyKeyboard(inlineKeyboardService.getInaccuracyKeyboard(new Locale(state.getLanguage())))
        );
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        inaccuracy(command, chatId, text);
    }
}
