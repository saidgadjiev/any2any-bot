package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.image.editor.StateFather;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ImageEditorCommand implements KeyboardBotCommand, NavigableBotCommand, CallbackBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEditorCommand.class);

    private Set<String> names = new HashSet<>();

    private final LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private ReplyKeyboardService replyKeyboardService;

    private StateFather stateFather;

    private FormatService formatService;

    @Autowired
    public ImageEditorCommand(LocalisationService localisationService,
                              @Qualifier("limits") MessageService messageService, UserService userService,
                              @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                              StateFather stateFather, FormatService formatService) {
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.stateFather = stateFather;
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
    public void cancel(long chatId, String queryId) {
        stateFather.cancel(this, chatId, queryId);
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
                        localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_TRANSPARENCY_WELCOME, locale))
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
            stateFather.initializeState(this, message.getChatId(), getEditFile(message, locale), locale);
        } else if (message.hasText()) {
            stateFather.userText(this, message.getChatId(), text);
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
        if (requestParams.contains(Arg.GO_BACK.getKey())) {
            stateFather.goBack(this, callbackQuery);
        }
        if (requestParams.contains(Arg.IMAGE_SIZE.getKey())) {
            stateFather.size(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), requestParams.getString(Arg.IMAGE_SIZE.getKey()));
        } else if (requestParams.contains(Arg.IMAGE_FILTER.getKey())) {
            State.Filter effect = State.Filter.valueOf(requestParams.getString(Arg.IMAGE_FILTER.getKey()));
            stateFather.applyFilter(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), effect);
        } else if (requestParams.contains(Arg.EDIT_STATE_NAME.getKey())) {
            State.Name name = State.Name.valueOf(requestParams.getString(Arg.EDIT_STATE_NAME.getKey()));
            stateFather.go(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), name);
        } else if (requestParams.contains(Arg.TRANSPARENT_MODE.getKey())) {
            ModeState.Mode mode = ModeState.Mode.valueOf(requestParams.getString(Arg.TRANSPARENT_MODE.getKey()));
            stateFather.transparentMode(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), mode);
        } else if (requestParams.contains(Arg.TRANSPARENT_COLOR.getKey())) {
            String color = requestParams.getString(Arg.TRANSPARENT_COLOR.getKey());
            stateFather.transparentColor(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), color);
        } else if (requestParams.contains(Arg.INACCURACY.getKey())) {
            stateFather.inaccuracy(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId(), requestParams.getString(Arg.INACCURACY.getKey()));
        }
    }

    @Override
    public void leave(long chatId) {
        stateFather.leave(this, chatId);
    }

    private boolean isMediaMessage(Message message) {
        return message.hasPhoto() || message.hasDocument();
    }

    private Any2AnyFile getEditFile(Message message, Locale locale) {
        Any2AnyFile any2AnyFile = new Any2AnyFile();

        if (message.hasDocument()) {
            any2AnyFile.setFileId(message.getDocument().getFileId());
            any2AnyFile.setFileName(message.getDocument().getFileName());
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            any2AnyFile.setFormat(checkFormat(format, message.getDocument().getMimeType(), message.getDocument().getFileName(), message.getDocument().getFileId(), locale));
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            any2AnyFile.setFileId(photoSize.getFileId());
            any2AnyFile.setFileName(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale));
            any2AnyFile.setFormat(formatService.getImageFormat(photoSize.getFileId()));
        }

        return any2AnyFile;
    }

    private Format checkFormat(Format format, String mimeType, String fileName, String fileId, Locale locale) {
        if (format == null) {
            if (StringUtils.isNotBlank(mimeType)) {
                LOGGER.debug("Format not resolved for " + mimeType + " and fileName " + fileName);
            } else {
                LOGGER.debug("Format not resolved for image " + fileId);
            }
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_IMAGE, locale));
        }
        if (format.getCategory() != FormatCategory.IMAGES) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_IMAGE, locale));
        }

        return format;
    }
}
