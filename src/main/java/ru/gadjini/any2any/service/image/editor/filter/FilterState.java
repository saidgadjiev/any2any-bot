package ru.gadjini.any2any.service.image.editor.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

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

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private ThreadPoolTaskExecutor executor;

    @Autowired
    public FilterState(CommandStateService commandStateService, ImageConvertDevice imageDevice,
                       TempFileService tempFileService, @Qualifier("limits") MessageService messageService,
                       InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                       @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.commandStateService = commandStateService;
        this.imageDevice = imageDevice;
        this.tempFileService = tempFileService;
        this.messageService = messageService;
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
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.deleteMessage(chatId, editorState.getMessageId());
        Locale locale = new Locale(editorState.getLanguage());
        SendFileResult sendFileResult = messageService.sendDocument(new SendDocument(chatId, editorState.getFileName(), new File(editorState.getCurrentFilePath()))
                .setReplyMarkup(inlineKeyboardService.getImageFiltersKeyboard(locale, editorState.canCancel())));

        editorState.setCurrentFileId(sendFileResult.getFileId());
        editorState.setMessageId(sendFileResult.getMessageId());
        commandStateService.setState(chatId, command.getHistoryName(), editorState);
        if (StringUtils.isNotBlank(queryId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.UPDATE_CALLBACK_ANSWER, locale)));
        }
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setCurrentFileId(editorState.getPrevFileId());
            editorState.setPrevFilePath(null);
            editorState.setPrevFileId(null);

            messageService.editMessageMedia(new EditMessageMedia(chatId, editorState.getMessageId(), editorState.getCurrentFileId())
                    .setReplyMarkup(inlineKeyboardService.getImageFiltersKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));

            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))));
        }
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageMedia(new EditMessageMedia(chatId, state.getMessageId(), state.getCurrentFileId())
                .setReplyMarkup(inlineKeyboardService.getImageFiltersKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }

    @Override
    public void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter effect) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        executor.execute(() -> {
            SmartTempFile result = tempFileService.getTempFile(chatId, editorState.getCurrentFileId(), TAG, Format.PNG.getExt());
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
            EditMediaResult editMediaResult = messageService.editMessageMedia(new EditMessageMedia(chatId,
                    editorState.getMessageId(), editorState.getFileName(), result.getFile())
                    .setReplyMarkup(inlineKeyboardService.getImageFiltersKeyboard(locale, editorState.canCancel())));
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackQuery(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_FILTER_APPLIED_ANSWER, locale))
                );
            }
        });
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        EditorState state = commandStateService.getState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), true);
        state.setStateName(imageEditorState.getName());
        imageEditorState.enter(command, callbackQuery.getMessage().getChatId());
        commandStateService.setState(callbackQuery.getMessage().getChatId(), command.getHistoryName(), state);
    }
}
