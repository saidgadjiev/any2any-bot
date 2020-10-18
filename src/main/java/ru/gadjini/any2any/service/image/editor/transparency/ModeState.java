package ru.gadjini.any2any.service.image.editor.transparency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

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
    public ModeState(@Qualifier("messageLimits") MessageService messageService, InlineKeyboardService inlineKeyboardService,
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
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(transparencyState.getName());
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.editMessageCaption(new EditMessageCaption(chatId, state.getMessageId(),
                messageBuilder.getSettingsStr(state) + "\n\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_MODE_WELCOME, new Locale(state.getLanguage())))
                .setReplyMarkup(inlineKeyboardService.getTransparentModeKeyboard(new Locale(state.getLanguage()))));
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, String queryId, Mode mode) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        Locale locale = new Locale(state.getLanguage());
        if (state.getMode() == mode) {
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(queryId,
                            localisationService.getMessage(MessagesProperties.MESSAGE_TRANSPARENCY_MODE_CHANGED, locale))
            );
            return;
        }
        state.setMode(mode);
        messageService.editMessageCaption(
                new EditMessageCaption(chatId, state.getMessageId(), messageBuilder.getSettingsStr(state) + "\n\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_MODE_WELCOME, locale))
                        .setReplyMarkup(inlineKeyboardService.getTransparentModeKeyboard(locale))
        );
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    public enum Mode {

        NEGATIVE,

        POSITIVE
    }
}
