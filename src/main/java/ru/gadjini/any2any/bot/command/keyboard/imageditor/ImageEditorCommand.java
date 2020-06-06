package ru.gadjini.any2any.bot.command.keyboard.imageditor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.image.editor.EditorState;
import ru.gadjini.any2any.service.image.editor.ImageEditorService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ImageEditorCommand implements KeyboardBotCommand, NavigableBotCommand, CallbackBotCommand {

    private Set<String> names = new HashSet<>();

    private final LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private ReplyKeyboardService replyKeyboardService;

    private ImageEditorService imageEditorService;

    private FormatService formatService;

    @Autowired
    public ImageEditorCommand(LocalisationService localisationService,
                              @Qualifier("limits") MessageService messageService, UserService userService,
                              @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                              ImageEditorService imageEditorService, FormatService formatService) {
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.imageEditorService = imageEditorService;
        this.formatService = formatService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.IMAGE_EDITOR_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        messageService.sendMessage(
                new SendMessageContext(message.getChatId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_WELCOME, locale))
                        .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale))
        );

        return true;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.IMAGE_EDITOR_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (isMediaMessage(message)) {
            imageEditorService.editFile(message.getChatId(), getEditFile(message, locale), locale);
        } else if (message.hasText()) {
            imageEditorService.transparentColor(message.getChatId(), text);
        }
    }

    @Override
    public String getName() {
        return CommandNames.IMAGE_EDITOR_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        return null;
    }

    @Override
    public void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(Arg.IMAGE_EDITOR_SCREEN.getKey())) {
            imageEditorService.changeScreen(callbackQuery.getMessage().getChatId(), EditorState.Screen.valueOf(requestParams.getString(Arg.IMAGE_EDITOR_SCREEN.getKey())));
        } else if (requestParams.contains(Arg.TRANSPARENT_MODE.getKey())) {
            EditorState.Mode mode = EditorState.Mode.valueOf(requestParams.getString(Arg.TRANSPARENT_MODE.getKey()));
            imageEditorService.transparentMode(callbackQuery.getMessage().getChatId(), mode);
        } else if (requestParams.contains(Arg.TRANSPARENT_COLOR.getKey())) {
            String color = requestParams.getString(Arg.TRANSPARENT_COLOR.getKey());
            imageEditorService.transparentColor(callbackQuery.getMessage().getChatId(), color);
        }
    }

    private boolean isMediaMessage(Message message) {
        return message.hasPhoto();
    }

    private Any2AnyFile getEditFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = new Any2AnyFile();

        PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
        any2AnyFile.setFileId(photoSize.getFileId());
        any2AnyFile.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale));
        any2AnyFile.setFormat(formatService.getImageFormat(photoSize.getFileId()));

        return any2AnyFile;
    }
}
