package ru.gadjini.any2any.service.image.editor.transparency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.EditMessageCaptionContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class ModeState implements State {

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private EditMessageBuilder messageBuilder;

    private TransparencyState transparencyState;

    private LocalisationService localisationService;

    @Autowired
    public ModeState(@Qualifier("limits") MessageService messageService, InlineKeyboardService inlineKeyboardService,
                     CommandStateService commandStateService, EditMessageBuilder messageBuilder, LocalisationService localisationService) {
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.messageBuilder = messageBuilder;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setTransparencyState(TransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Override
    public Name getName() {
        return Name.MODE;
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(transparencyState.getName());
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageCaption(new EditMessageCaptionContext(chatId, state.getMessageId(),
                messageBuilder.getSettingsStr(state) + "\n\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_MODE_WELCOME, new Locale(state.getLanguage())))
                .replyKeyboard(inlineKeyboardService.getTransparentModeKeyboard(new Locale(state.getLanguage()))));
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, Mode mode) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        state.setMode(mode);
        messageService.editMessageCaption(
                new EditMessageCaptionContext(chatId, state.getMessageId(), messageBuilder.getSettingsStr(state))
                        .replyKeyboard(inlineKeyboardService.getTransparentModeKeyboard(new Locale(state.getLanguage())))
        );
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    public enum Mode {

        NEGATIVE,

        POSITIVE
    }
}
