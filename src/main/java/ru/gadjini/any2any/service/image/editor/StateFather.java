package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Locale;
import java.util.Set;

@Service
public class StateFather implements State {

    private Set<State> states;

    private CommandStateService commandStateService;

    private CommonJobExecutor commonJobExecutor;

    private MessageService messageService;

    private TempFileService fileService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private TelegramService telegramService;

    private ImageDevice imageDevice;

    @Autowired
    public StateFather(CommandStateService commandStateService, CommonJobExecutor commonJobExecutor,
                       @Qualifier("limits") MessageService messageService, TempFileService fileService,
                       LocalisationService localisationService,
                       InlineKeyboardService inlineKeyboardService,
                       TelegramService telegramService, ImageDevice imageDevice) {
        this.commandStateService = commandStateService;
        this.commonJobExecutor = commonJobExecutor;
        this.messageService = messageService;
        this.fileService = fileService;
        this.localisationService = localisationService;
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
    public void applyEffect(ImageEditorCommand command, long chatId, String queryId, Effect effect) {
        getState(chatId, command.getHistoryName()).applyEffect(command, chatId, queryId, effect);
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, Name name) {
        getState(chatId, command.getHistoryName()).go(command, chatId, name);
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        getState(callbackQuery.getMessage().getChatId(), command.getHistoryName()).goBack(command, callbackQuery);
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, ModeState.Mode mode) {
        getState(chatId, command.getHistoryName()).transparentMode(command, chatId, mode);
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {
        getState(chatId, command.getHistoryName()).transparentColor(command, chatId, queryId, text);
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String inaccuracy) {
        getState(chatId, command.getHistoryName()).inaccuracy(command, chatId, inaccuracy);
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        getState(chatId, command.getHistoryName()).cancel(command, chatId, queryId);
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        getState(chatId, command.getHistoryName()).userText(command, chatId, text);
    }

    public void initializeState(ImageEditorCommand command, long chatId, Any2AnyFile any2AnyFile, Locale locale) {
        commonJobExecutor.addJob(() -> {
            SmartTempFile file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(any2AnyFile.getFileName(), any2AnyFile.getFormat().getExt()));
            telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file.getFile());
            try {
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(file.getName(), Format.PNG.getExt()));
                imageDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());
                EditorState state = createState(result.getAbsolutePath(), result.getName());
                state.setLanguage(locale.getLanguage());
                SendFileResult fileResult = messageService.sendDocument(new SendFileContext(chatId, result.getFile())
                        .replyKeyboard(inlineKeyboardService.getImageEditKeyboard(locale, state.canCancel())));
                state.setMessageId(fileResult.getMessageId());
                state.setFileId(fileResult.getFileId());
                commandStateService.setState(chatId, command.getHistoryName(), state);
            } finally {
                file.smartDelete();
            }
        });
    }

    private State getState(long chatId, String commandName) {
        EditorState state = commandStateService.getState(chatId, commandName, true);

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
}
