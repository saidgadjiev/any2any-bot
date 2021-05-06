package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.image.editor.filter.FilterState;
import ru.gadjini.any2any.service.image.editor.transparency.TransparencyState;
import ru.gadjini.any2any.service.image.resize.ResizeState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.File;
import java.util.Locale;

@SuppressWarnings("CPD-START")
@Component
public class ImageEditorState implements State {

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private TransparencyState transparencyState;

    private FilterState filterState;

    private ResizeState resizeState;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ImageEditorState(CommandStateService commandStateService,
                            LocalisationService localisationService, @TgMessageLimitsControl MessageService messageService,
                            @Qualifier("mediaLimits") MediaMessageService mediaMessageService, InlineKeyboardService inlineKeyboardService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Autowired
    public void setTransparencyState(TransparencyState transparencyState) {
        this.transparencyState = transparencyState;
    }

    @Autowired
    public void setFilterState(FilterState filterState) {
        this.filterState = filterState;
    }

    @Autowired
    public void setResizeState(ResizeState resizeState) {
        this.resizeState = resizeState;
    }

    @Override
    public Name getName() {
        return Name.EDIT;
    }

    @Override
    public void update(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.deleteMessage(chatId, editorState.getMessageId());
        Locale locale = new Locale(editorState.getLanguage());

        SendFileResult sendFileResult = mediaMessageService.sendDocument(SendDocument.builder()
                .chatId(String.valueOf(chatId))
                .document(new InputFile(editorState.getCurrentFileId()))
                .caption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_WELCOME, locale))
                .replyMarkup(inlineKeyboardService.getImageEditKeyboard(locale, editorState.canCancel()))
                .build());

        editorState.setMessageId(sendFileResult.getMessageId());
        commandStateService.setState(chatId, command.getHistoryName(), editorState);
        if (StringUtils.isNotBlank(queryId)) {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.UPDATE_CALLBACK_ANSWER, locale)).build());
        }
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setCurrentFileId(editorState.getPrevFileId());
            editorState.setPrevFilePath(null);
            editorState.setPrevFileId(null);

            mediaMessageService.editMessageMedia(EditMessageMedia.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(new InputMediaDocument(editorState.getCurrentFileId()))
                    .replyMarkup(inlineKeyboardService.getImageEditKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel()))
                    .build());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))).build());
        }
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, String queryId, Name name) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);

        switch (name) {
            case TRANSPARENCY:
                transparencyState.enter(command, chatId);
                state.setStateName(transparencyState.getName());
                break;
            case FILTERS:
                filterState.enter(command, chatId);
                state.setStateName(filterState.getName());
                break;
            case RESIZE:
                resizeState.enter(command, chatId);
                state.setStateName(resizeState.getName());
                break;
        }
        commandStateService.setState(chatId, command.getHistoryName(), state);
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        Locale locale = new Locale(state.getLanguage());
        mediaMessageService.editMessageMedia(EditMessageMedia.builder()
                .chatId(String.valueOf(chatId))
                .messageId(state.getMessageId())
                .media(InputMediaDocument.builder().media(state.getCurrentFileId()).caption(
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_WELCOME, locale)).build())
                .replyMarkup(inlineKeyboardService.getImageEditKeyboard(locale, state.canCancel())).build());
    }
}
