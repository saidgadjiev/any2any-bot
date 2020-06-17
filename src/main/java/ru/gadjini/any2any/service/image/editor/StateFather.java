package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Locale;
import java.util.Set;

@Service
public class StateFather implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateFather.class);

    private Set<State> states;

    private CommandStateService commandStateService;

    private CommonJobExecutor commonJobExecutor;

    private MessageService messageService;

    private TempFileService tempFileService;

    private InlineKeyboardService inlineKeyboardService;

    private TelegramService telegramService;

    private ImageConvertDevice imageDevice;

    @Autowired
    public StateFather(CommandStateService commandStateService, CommonJobExecutor commonJobExecutor,
                       @Qualifier("limits") MessageService messageService, TempFileService tempFileService,
                       InlineKeyboardService inlineKeyboardService,
                       TelegramService telegramService, ImageConvertDevice imageDevice) {
        this.commandStateService = commandStateService;
        this.commonJobExecutor = commonJobExecutor;
        this.messageService = messageService;
        this.tempFileService = tempFileService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.telegramService = telegramService;
        this.imageDevice = imageDevice;
    }

    @Autowired
    public void setStates(Set<State> states) {
        this.states = states;
    }

    @Override
    public Name getName() {
        return Name.FATHER;
    }

    @Override
    public void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter filter) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#applyFilter(" + filter + ")");

        state.applyFilter(command, chatId, queryId, filter);
    }

    @Override
    public void size(ImageEditorCommand command, long chatId, String queryId, String size) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#size(" + size + ")");

        state.size(command, chatId, queryId, size);
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, String queryId, Name name) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#go(" + name + ")");

        state.go(command, chatId, queryId, name);
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        getState(callbackQuery.getMessage().getChatId(), command.getHistoryName()).goBack(command, callbackQuery);
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, String queryId, ModeState.Mode mode) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#transparentMode(" + mode + ")");

        state.transparentMode(command, chatId, queryId, mode);
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#transparentColor(" + text + ")");

        state.transparentColor(command, chatId, queryId, text);
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String queryId, String inaccuracy) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#inaccuracy(" + inaccuracy + ")");

        state.inaccuracy(command, chatId, queryId, inaccuracy);
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#cancel");

        state.cancel(command, chatId, queryId);
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#userText(" + text + ")");

        state.userText(command, chatId, text);
    }

    public void initializeState(ImageEditorCommand command, long chatId, Any2AnyFile any2AnyFile, Locale locale) {
        commonJobExecutor.addJob(() -> {
            SmartTempFile file = tempFileService.createTempFile(Any2AnyFileNameUtils.getFileName(any2AnyFile.getFileName(), any2AnyFile.getFormat().getExt()));
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file.getFile());
            try {
                SmartTempFile result = tempFileService.createTempFile(Any2AnyFileNameUtils.getFileName(file.getName(), Format.PNG.getExt()));
                imageDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());
                EditorState state = createState(result.getAbsolutePath(), result.getName());
                state.setLanguage(locale.getLanguage());
                SendFileResult fileResult = messageService.sendDocument(new SendFileContext(chatId, result.getFile())
                        .replyKeyboard(inlineKeyboardService.getImageEditKeyboard(locale, state.canCancel())));
                state.setMessageId(fileResult.getMessageId());
                state.setCurrentFileId(fileResult.getFileId());
                deleteCurrentState(chatId, command.getHistoryName());
                commandStateService.setState(chatId, command.getHistoryName(), state);
                LOGGER.debug("Image editor state initialized for user " + chatId + " file id " + any2AnyFile.getFileId() + " format " + any2AnyFile.getFormat());
            } finally {
                file.smartDelete();
            }
        });
    }

    public void leave(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), false);
        if (state == null) {
            LOGGER.debug("Image editor state is empty");
            return;
        }
        try {
            messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getCurrentFileId()));
            commandStateService.deleteState(chatId, command.getHistoryName());
            LOGGER.debug("Image editor state deleted for user " + chatId);
        } finally {
            if (StringUtils.isNotBlank(state.getPrevFilePath())) {
                new SmartTempFile(new File(state.getPrevFilePath()), true).smartDelete();
            }

            new SmartTempFile(new File(state.getCurrentFilePath()), true).smartDelete();
        }
    }

    private State getState(long chatId, String commandName) {
        EditorState state = getEditorState(chatId, commandName);

        return findState(state.getStateName());
    }

    private State findState(State.Name name) {
        return states.stream().filter(state -> state.getName().equals(name)).findFirst().orElseThrow();
    }

    private EditorState createState(String filePath, String fileName) {
        EditorState editorState = new EditorState();
        editorState.setImage(filePath);
        editorState.setFileName(fileName);

        return editorState;
    }

    private void deleteCurrentState(long chatId, String commandName) {
        EditorState state = commandStateService.getState(chatId, commandName, false);

        if (state != null) {
            messageService.editMessageMedia(new EditMediaContext(chatId, state.getMessageId(), state.getCurrentFileId()));
        }
    }

    private EditorState getEditorState(long chatId, String commandName) {
        EditorState editorState = commandStateService.getState(chatId, commandName, true);

        try {
            telegramService.restoreFileIfNeed(editorState.getCurrentFilePath(), editorState.getCurrentFileId());
            if (StringUtils.isNotBlank(editorState.getPrevFilePath())) {
                telegramService.restoreFileIfNeed(editorState.getPrevFilePath(), editorState.getPrevFileId());
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        return editorState;
    }
}
