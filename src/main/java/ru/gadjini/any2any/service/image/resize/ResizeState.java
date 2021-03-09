package ru.gadjini.any2any.service.image.resize;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.gadjini.any2any.service.image.device.ImageIdentifyDevice;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorState;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
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
import java.util.regex.Pattern;

@SuppressWarnings("CPD-START")
@Component
public class ResizeState implements State {

    public static final String TAG = "resize";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResizeState.class);

    private static final Pattern SIZE_PATTERN = Pattern.compile("\\d*\\.?\\d+[xXхХ]\\d*\\.?\\d+");

    private CommandStateService commandStateService;

    private ImageEditorState imageEditorState;

    private ImageConvertDevice imageDevice;

    private ImageIdentifyDevice identifyDevice;

    private TempFileService tempFileService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private InlineKeyboardService inlineKeyboardService;

    private LocalisationService localisationService;

    private ThreadPoolTaskExecutor executor;

    @Autowired
    public ResizeState(CommandStateService commandStateService, ImageConvertDevice imageDevice,
                       ImageIdentifyDevice identifyDevice, TempFileService tempFileService,
                       @Qualifier("messageLimits") MessageService messageService,
                       @Qualifier("mediaLimits") MediaMessageService mediaMessageService, InlineKeyboardService inlineKeyboardService, LocalisationService localisationService,
                       @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.commandStateService = commandStateService;
        this.imageDevice = imageDevice;
        this.identifyDevice = identifyDevice;
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
        return Name.RESIZE;
    }

    @Override
    public void update(ImageEditorCommand command, long chatId, String queryId) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);

        messageService.deleteMessage(chatId, editorState.getMessageId());
        String size = identifyDevice.getSize(editorState.getCurrentFilePath());
        Locale locale = new Locale(editorState.getLanguage());

        SendFileResult sendFileResult = mediaMessageService.sendDocument(SendDocument.builder()
                .chatId(String.valueOf(chatId)).document(new InputFile(new File(editorState.getCurrentFilePath(), editorState.getFileName())))
                .caption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_SIZE, new Object[]{size}, locale) + "\n\n" +
                        localisationService.getMessage(MessagesProperties.MESSAGE_RESIZE_IMAGE_WELCOME, locale))
                .replyMarkup(inlineKeyboardService.getResizeKeyboard(locale, editorState.canCancel())).build());
        editorState.setMessageId(sendFileResult.getMessageId());
        editorState.setCurrentFileId(sendFileResult.getFileId());
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

            String size = identifyDevice.getSize(editorState.getCurrentFilePath());
            Locale locale = new Locale(editorState.getLanguage());
            mediaMessageService.editMessageMedia(EditMessageMedia.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(InputMediaDocument.builder().media(editorState.getCurrentFileId()).caption(
                            localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_SIZE, new Object[]{size}, locale) + "\n\n" +
                                    localisationService.getMessage(MessagesProperties.MESSAGE_RESIZE_IMAGE_WELCOME, locale)).build())
                    .replyMarkup(inlineKeyboardService.getResizeKeyboard(new Locale(editorState.getLanguage()), editorState.canCancel())).build());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            new SmartTempFile(new File(editFilePath)).smartDelete();
        } else {
            messageService.sendAnswerCallbackQuery(AnswerCallbackQuery.builder()
                    .callbackQueryId(queryId)
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_CANT_CANCEL_ANSWER, new Locale(editorState.getLanguage())))
                    .showAlert(true)
                    .build());
        }
    }

    @Override
    public void enter(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        String size = identifyDevice.getSize(state.getCurrentFilePath());
        Locale locale = new Locale(state.getLanguage());
        mediaMessageService.editMessageMedia(EditMessageMedia.builder()
                .chatId(String.valueOf(chatId))
                .messageId(state.getMessageId())
                .media(InputMediaDocument.builder().media(state.getCurrentFileId())
                        .caption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_SIZE, new Object[]{size}, locale) + "\n\n" +
                                localisationService.getMessage(MessagesProperties.MESSAGE_RESIZE_IMAGE_WELCOME, locale)).build())
                .replyMarkup(inlineKeyboardService.getResizeKeyboard(new Locale(state.getLanguage()), state.canCancel())).build());
    }

    @Override
    public void size(ImageEditorCommand command, long chatId, String queryId, String srcSize) {
        EditorState editorState = commandStateService.getState(chatId, command.getHistoryName(), true, EditorState.class);
        validateSize(srcSize, new Locale(editorState.getLanguage()));
        executor.execute(() -> {
            String targetSize = forceSize(srcSize);

            SmartTempFile result = tempFileService.getTempFile(FileTarget.TEMP, chatId, editorState.getCurrentFileId(), TAG, Format.PNG.getExt());
            imageDevice.resize(editorState.getCurrentFilePath(), result.getAbsolutePath(), targetSize);
            if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
                SmartTempFile prevFile = new SmartTempFile(new File(editorState.getPrevFilePath()));
                prevFile.smartDelete();
            }
            editorState.setPrevFilePath(editorState.getCurrentFilePath());
            editorState.setPrevFileId(editorState.getCurrentFileId());
            editorState.setCurrentFilePath(result.getAbsolutePath());
            Locale locale = new Locale(editorState.getLanguage());
            String newSize = identifyDevice.getSize(editorState.getCurrentFilePath());
            InputMediaDocument inputMediaDocument = new InputMediaDocument();
            inputMediaDocument.setMedia(result.getFile(), editorState.getFileName());
            inputMediaDocument.setCaption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_SIZE, new Object[]{newSize}, locale) + "\n\n" +
                    localisationService.getMessage(MessagesProperties.MESSAGE_RESIZE_IMAGE_WELCOME, locale));
            EditMediaResult editMediaResult = mediaMessageService.editMessageMedia(EditMessageMedia.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(editorState.getMessageId())
                    .media(inputMediaDocument)
                    .replyMarkup(inlineKeyboardService.getResizeKeyboard(locale, editorState.canCancel())).build());
            editorState.setCurrentFileId(editMediaResult.getFileId());
            commandStateService.setState(chatId, command.getHistoryName(), editorState);

            if (StringUtils.isNotBlank(queryId)) {
                messageService.sendAnswerCallbackQuery(
                        AnswerCallbackQuery.builder().callbackQueryId(queryId)
                                .text(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGES_RESIZED_ANSWER, locale))
                                .build()
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

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        size(command, chatId, null, text);
    }

    private String forceSize(String size) {
        if (size.endsWith(">")) {
            return size;
        }

        return size + ">";
    }

    private void validateSize(String size, Locale locale) {
        if (SIZE_PATTERN.matcher(size).matches()) {
            return;
        }

        LOGGER.warn("Incorrect size({})", size);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_IMAGE_SIZE, locale));
    }
}
