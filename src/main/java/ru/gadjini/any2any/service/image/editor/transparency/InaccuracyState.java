package ru.gadjini.any2any.service.image.editor.transparency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.image.editor.EditMessageBuilder;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class InaccuracyState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(InaccuracyState.class);

    private TransparencyState transparencyState;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private EditMessageBuilder messageBuilder;

    private LocalisationService localisationService;

    @Autowired
    public InaccuracyState(CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService,
                           @TgMessageLimitsControl MessageService messageService, EditMessageBuilder messageBuilder, LocalisationService localisationService) {
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageService = messageService;
        this.messageBuilder = messageBuilder;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setTransparencyState(TransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Override
    public Name getName() {
        return Name.INACCURACY;
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        transparencyState.enter(command, callbackQuery.getMessage().getChatId());
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(transparencyState.getName());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.editMessageCaption(EditMessageCaption.builder().chatId(String.valueOf(chatId)).messageId(state.getMessageId())
                .caption(messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_INACCURACY_WELCOME, new Locale(state.getLanguage())))
                .replyMarkup(inlineKeyboardService.getInaccuracyKeyboard(new Locale(state.getLanguage()))).build());
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String queryId, String inaccuracy) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        Locale locale = new Locale(state.getLanguage());
        inaccuracy = validateAndGet(inaccuracy, locale);
        if (state.getInaccuracy().equals(inaccuracy)) {
            messageService.sendAnswerCallbackQuery(
                    AnswerCallbackQuery.builder().callbackQueryId(queryId)
                            .text(localisationService.getMessage(MessagesProperties.MESSAGE_INACCURACY_CHANGED, locale)).build()
            );
            return;
        }
        state.setInaccuracy(inaccuracy);
        messageService.editMessageCaption(
                EditMessageCaption.builder().chatId(String.valueOf(chatId)).messageId(state.getMessageId()).caption(messageBuilder.getSettingsStr(state) + "\n\n"
                        + localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_INACCURACY_WELCOME, locale))
                        .replyMarkup(inlineKeyboardService.getInaccuracyKeyboard(locale)).build()
        );
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        inaccuracy(command, chatId, null, text);
    }

    private String cleanUp(String inaccuracy) {
        return inaccuracy.replace("%", "");
    }

    private String validateAndGet(String inaccuracy, Locale locale) {
        inaccuracy = cleanUp(inaccuracy);
        double v;
        try {
            v = Double.parseDouble(inaccuracy);
        } catch (NumberFormatException ex) {
            LOGGER.warn("NF({})", inaccuracy);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_INACCURACY, locale));
        }
        if (v < 0 || v > 100) {
            LOGGER.warn("Incorrect({})", inaccuracy);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_INACCURACY, locale));
        }

        return String.valueOf(v);
    }
}
