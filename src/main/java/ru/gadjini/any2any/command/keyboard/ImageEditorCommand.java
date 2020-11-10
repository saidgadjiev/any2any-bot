package ru.gadjini.any2any.command.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.image.editor.StateFather;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.any2any.service.keyboard.Any2AnyReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ImageEditorCommand implements KeyboardBotCommand, NavigableBotCommand, CallbackBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEditorCommand.class);

    private Set<String> names = new HashSet<>();

    private final LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private Any2AnyReplyKeyboardService replyKeyboardService;

    private StateFather stateFather;

    private MessageMediaService messageMediaService;

    @Autowired
    public ImageEditorCommand(LocalisationService localisationService,
                              @Qualifier("messageLimits") MessageService messageService, UserService userService,
                              @Qualifier("curr") Any2AnyReplyKeyboardService replyKeyboardService,
                              StateFather stateFather, MessageMediaService messageMediaService) {
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.stateFather = stateFather;
        this.messageMediaService = messageMediaService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.IMAGE_EDITOR_COMMAND_NAME, locale));
        }
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
    public void processMessage(Message message, String[] params) {
        processMessage(message, (String) null);
    }

    @Override
    public String getCommandIdentifier() {
        return FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return true;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME;
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
        return FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
    }

    @Override
    public void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {
        if (requestParams.contains(Arg.GO_BACK.getKey())) {
            stateFather.goBack(this, callbackQuery);
        } else if (requestParams.contains(Arg.UPDATE_EDITED_IMAGE.getKey())) {
            stateFather.update(this, callbackQuery.getMessage().getChatId(), callbackQuery.getId());
        } else if (requestParams.contains(Arg.IMAGE_SIZE.getKey())) {
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

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);

        messageService.sendMessage(
                SendMessage.builder().chatId(String.valueOf(chatId))
                        .parseMode(ParseMode.HTML)
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_MAIN_WELCOME, locale))
                        .replyMarkup(replyKeyboardService.goBack(chatId, locale)).build()
        );
    }

    private MessageMedia getEditFile(Message message, Locale locale) {
        MessageMedia any2AnyFile = messageMediaService.getMedia(message, locale);

        if (message.hasDocument()) {
            checkFormat(message.getFrom().getId(), any2AnyFile.getFormat(), any2AnyFile.getMimeType(), any2AnyFile.getFileName(), locale);
        } else if (any2AnyFile == null || any2AnyFile.getFormat().getCategory() != FormatCategory.IMAGES) {
            LOGGER.debug("No image({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_IMAGE_EDITOR_MAIN_WELCOME, userService.getLocaleOrDefault(message.getFrom().getId())));
        }

        return any2AnyFile;
    }

    private void checkFormat(int userId, Format format, String mimeType, String fileName, Locale locale) {
        if (format == null) {
            LOGGER.warn("Format is null({}, {}, {})", userId, mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_IMAGE, locale));
        }
        if (format.getCategory() != FormatCategory.IMAGES) {
            LOGGER.warn("No image({}, {})", userId, format.getCategory());
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_BAD_IMAGE, locale));
        }
    }
}
