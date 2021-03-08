package ru.gadjini.any2any.service.image.editor.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import ru.gadjini.any2any.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.EditMediaResult;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.io.File;
import java.util.Locale;

@Component
public class FilterState implements State {

    public static final String TAG = "filter";

    private CommandStateService commandStateService;

    private ImageEditorState imageEditorState;

    private ImageConvertDevice imageDevice;

    private TempFileService tempFileService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private ThreadPoolTaskExecutor executor;

    @Autowired
    public FilterState(CommandStateService commandStateService, ImageConvertDevice imageDevice,
                       TempFileService tempFileService, @Qualifier("messageLimits") MessageService messageService,
                       @Qualifier("mediaLimits") MediaMessageService mediaMessageService, InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                       @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.commandStateService = commandStateService;
        this.imageDevice = imageDevice;
        this.tempFileService = tempFileService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
        this.executor = executor;
    }

    @Autowired
    public void setImageEditorState(ImageEditorState imageEditorState) {
        this.imageEditorState = imageEditorState;
    }

    @Override
    public Name getName() {
        return Name.FILTERS;
    }

    @Override
    public void update(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        messageService.deleteMessage(chatId, editorState.getMessageId());
        Locale locale = new Locale(editorState.getLanguage());
        SendFileResult sendFileResult = mediaMessageService.sendDocument(SendDocument.builder()
                .chatId(String.valueOf(chatId))
                .document(new InputFile(new File(editorState.getCurrentFilePath()), editorState.getFileName()))
                .replyMarkup(inlineKeyboardService.getImageFiltersKeyboard(locale, editorState.canCancel())).build());

        editorState.setCurrentFileId(sendFileResult.getFileId());
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

            mediaMessageService.editMessageMedia(EditMessageMedia.builder().chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(new InputMediaDocument(editorState.getCurrentFileId()))
                    .replyMarkup(inlineKeyboardService.getImageFiltersKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())).build());

            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder().callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))).build());
        }
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        mediaMessageService.editMessageMedia(EditMessageMedia.builder().chatId(String.valueOf(chatId))
                .messageId(state.getMessageId())
                .media(new InputMediaDocument(state.getCurrentFileId()))
                .replyMarkup(inlineKeyboardService.getImageFiltersKeyboard(new Locale(state.getLanguage()), state.canCancel())).build());
    }

    @Override
    public void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter effect) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        executor.execute(() -> {
            SmartTempFile result = tempFileService.getTempFile(FileTarget.TEMP, chatId, editorState.getCurrentFileId(), TAG, Format.PNG.getExt());
            switch (effect) {
                case SKETCH:
                    imageDevice.applySketchFilter(editorState.getCurrentFilePath(), result.getAbsolutePath());
                    break;
                case BLACK_AND_WHITE:
                    imageDevice.applyBlackAndWhiteFilter(editorState.getCurrentFilePath(), result.getAbsolutePath());
                    break;
                case NEGATIVE:
                    imageDevice.applyNegativeFilter(editorState.getCurrentFilePath(), result.getAbsolutePath());
                    break;
            }
            if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevFilePath()));
                prevFile.smartDelete();
            }
            editorState.setPrevFilePath(editorState.getCurrentFilePath());
            editorState.setPrevFileId(editorState.getCurrentFileId());
            editorState.setCurrentFilePath(result.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            InputMediaDocument inputMediaDocument = new InputMediaDocument();
            inputMediaDocument.setMedia(result.getFile(), editorState.getFileName());
            EditMediaResult editMediaResult = mediaMessageService.editMessageMedia(EditMessageMedia.builder().chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(inputMediaDocument)
                    .replyMarkup(inlineKeyboardService.getImageFiltersKeyboard(locale, editorState.canCancel())).build());
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        AnswerCallbackQuery.builder().callbackQueryId(queryId)
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_FILTER_APPLIED_ANSWER, locale)).build()
                );
            }
        });
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true, EditorState.class);
        state.setStateName(imageEditorState.getName());
        imageEditorState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }
}
