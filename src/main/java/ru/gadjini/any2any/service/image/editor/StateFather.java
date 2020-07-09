package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Locale;
import java.util.Set;

@Service
public class StateFather implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateFather.class);

    private Set<State> states;

    private CommandStateService commandStateService;

    private ThreadPoolTaskExecutor executor;

    private MessageService messageService;

    private TempFileService tempFileService;

    private InlineKeyboardService inlineKeyboardService;

    private TelegramService telegramService;

    private ImageConvertDevice imageDevice;

    private LocalisationService localisationService;

    @Autowired
    public StateFather(CommandStateService commandStateService,  @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                       @Qualifier("limits") MessageService messageService, TempFileService tempFileService,
                       InlineKeyboardService inlineKeyboardService,
                       TelegramService telegramService, ImageConvertDevice imageDevice, LocalisationService localisationService) {
        this.commandStateService = commandStateService;
        this.executor = executor;
        this.messageService = messageService;
        this.tempFileService = tempFileService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.telegramService = telegramService;
        this.imageDevice = imageDevice;
        this.localisationService = localisationService;
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
        LOGGER.debug(state.getClass().getSimpleName() + "#applyFilter({}, {})", chatId, filter);

        state.applyFilter(command, chatId, queryId, filter);
    }

    @Override
    public void size(ImageEditorCommand command, long chatId, String queryId, String size) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#size({}, {})", chatId, size);

        state.size(command, chatId, queryId, size);
    }

    @Override
    public void update(ImageEditorCommand command, long chatId, String queryId) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#update({})", chatId);

        state.update(command, chatId, queryId);
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, String queryId, Name name) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#go({}, {})", chatId, name);

        state.go(command, chatId, queryId, name);
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        State state = getState(callbackQuery.getMessage().getChatId(), command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#goBack({})", callbackQuery.getMessage().getChatId());

        state.goBack(command, callbackQuery);
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, String queryId, ModeState.Mode mode) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#transparentMode({}, {})", chatId, mode);

        state.transparentMode(command, chatId, queryId, mode);
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#transparentColor({}, {})", chatId, text);

        state.transparentColor(command, chatId, queryId, text);
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String queryId, String inaccuracy) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#inaccuracy({}, {})",chatId, inaccuracy);

        state.inaccuracy(command, chatId, queryId, inaccuracy);
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#cancel({})", chatId);

        state.cancel(command, chatId, queryId);
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        State state = getState(chatId, command.getHistoryName());
        LOGGER.debug(state.getClass().getSimpleName() + "#userText({}, {})", chatId, text);

        state.userText(command, chatId, text);
    }

    public void initializeState(ImageEditorCommand command, long chatId, Any2AnyFile any2AnyFile, Locale locale) {
        executor.execute(() -> {
            deleteCurrentState(chatId, command.getHistoryName());

            SmartTempFile file = tempFileService.createTempFile(Any2AnyFileNameUtils.getFileName(any2AnyFile.getFileName(), any2AnyFile.getFormat().getExt()));
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file);
            try {
                SmartTempFile result = tempFileService.createTempFile(Any2AnyFileNameUtils.getFileName(file.getName(), Format.PNG.getExt()));
                imageDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());
                EditorState state = createState(result.getAbsolutePath(), result.getName());
                state.setLanguage(locale.getLanguage());
                SendFileResult fileResult = messageService.sendDocument(new SendDocument(chatId, result.getFile())
                        .setCaption(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_WELCOME, locale))
                        .setReplyMarkup(inlineKeyboardService.getImageEditKeyboard(locale, state.canCancel())));
                state.setMessageId(fileResult.getMessageId());
                state.setCurrentFileId(fileResult.getFileId());
                commandStateService.setState(chatId, command.getHistoryName(), state);
                LOGGER.debug("New state({}, {}, {})", chatId, any2AnyFile.getFormat(), any2AnyFile.getFileId());
            } finally {
                file.smartDelete();
            }
        });
    }

    public void leave(ImageEditorCommand command, long chatId) {
        EditorState state = commandStateService.getState(chatId, command.getHistoryName(), false);
        if (state == null) {
            LOGGER.debug("Empty state({})", chatId);
            return;
        }
        try {
            File file = new File(state.getCurrentFilePath());
            if (file.length() > 0) {
                messageService.sendDocument(new SendDocument(chatId, new File(state.getCurrentFilePath())));
                messageService.deleteMessage(chatId, state.getMessageId());
            } else {
                messageService.editMessageMedia(new EditMessageMedia(chatId, state.getMessageId(), state.getCurrentFileId()));
            }
            commandStateService.deleteState(chatId, command.getHistoryName());
            LOGGER.debug("State deleted({})", chatId);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
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
            try {
                File file = new File(state.getCurrentFilePath());
                if (file.length() > 0) {
                    messageService.sendDocument(new SendDocument(chatId, new File(state.getCurrentFilePath())));
                    messageService.deleteMessage(chatId, state.getMessageId());
                } else {
                    messageService.editMessageMedia(new EditMessageMedia(chatId, state.getMessageId(), state.getCurrentFileId()));
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
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
