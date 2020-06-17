package ru.gadjini.any2any.service.image.editor.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.AnswerCallbackContext;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.io.File;
import java.util.Locale;

@Component
public class FilterState implements State {

    private CommandStateService commandStateService;

    private ImageEditorState imageEditorState;

    private ImageConvertDevice imageDevice;

    private TempFileService tempFileService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private CommonJobExecutor commonJobExecutor;

    @Autowired
    public FilterState(CommandStateService commandStateService, ImageConvertDevice imageDevice,
                       TempFileService tempFileService, @Qualifier("limits") MessageService messageService,
                       InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                       CommonJobExecutor commonJobExecutor) {
        this.commandStateService = commandStateService;
        this.imageDevice = imageDevice;
        this.tempFileService = tempFileService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localisationService = localisationService;
        this.commonJobExecutor = commonJobExecutor;
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
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
            String editFilePath = editorState.getCurrentFilePath();
            editorState.setCurrentFilePath(editorState.getPrevFilePath());
            editorState.setCurrentFileId(editorState.getPrevFileId());
            editorState.setPrevFilePath(null);
            editorState.setPrevFileId(null);

            EditMediaResult editMediaResult = messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), editorState.getCurrentFileId())
                    .replyKeyboard(inlineKeyboardService.getImageFiltersKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())));
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath), true).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackContext(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage()))));
        }
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true);
        messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getCurrentFileId())
                .replyKeyboard(inlineKeyboardService.getImageFiltersKeyboard(new Locale(state.getLanguage()), state.canCancel())));
    }

    @Override
    public void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter effect) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true);
        commonJobExecutor.addJob(() -> {
            SmartTempFile result = tempFileService.getTempFile(editorState.getFileName());
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
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevFilePath()), true);
                prevFile.smartDelete();
            }
            editorState.setPrevFilePath(editorState.getCurrentFilePath());
            editorState.setPrevFileId(editorState.getCurrentFileId());
            editorState.setCurrentFilePath(result.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            EditMediaResult editMediaResult = messageService.editMessageMedia(new EditMediaContext(chatId, editorState.getMessageId(), result.getFile())
                    .replyKeyboard(inlineKeyboardService.getImageFiltersKeyboard(locale, editorState.canCancel())));
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackContext(queryId, localisationService.getMessage(MessagesProperties.MESSAGE_FILTER_APPLIED_ANSWER, locale))
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
