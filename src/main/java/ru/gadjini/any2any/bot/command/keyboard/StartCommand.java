package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.filequeue.FileQueueMessageBuilder;
import ru.gadjini.any2any.service.filequeue.FileQueueService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class StartCommand extends BotCommand implements KeyboardBotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartCommand.class);

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private UserService userService;

    private FileQueueService fileQueueService;

    private FileQueueMessageBuilder queueMessageBuilder;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private FormatService formatService;

    private TelegramService telegramService;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService, FileQueueService fileQueueService,
                        FileQueueMessageBuilder queueMessageBuilder, @Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                        @Qualifier("currkeyboard") ReplyKeyboardService replyKeyboardService, FormatService formatService,
                        TelegramService telegramService) {
        super(CommandNames.START_COMMAND, "");
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.fileQueueService = fileQueueService;
        this.queueMessageBuilder = queueMessageBuilder;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.formatService = formatService;
        this.telegramService = telegramService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        Locale locale = userService.getLocale(user.getId());
        messageService.sendMessage(
                new SendMessageContext(chat.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                        .replyKeyboard(replyKeyboardService.getMainMenu(chat.getId(), locale))
        );
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocale(message.getFrom().getId());

        if (!commandStateService.hasState(message.getChatId())) {
            check(message, locale);
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    new SendMessageContext(message.getChatId(), queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .replyKeyboard(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale))
            );
            convertState.deleteWarns();
            commandStateService.setState(message.getChatId(), convertState);
        } else if (isMediaMessage(message)) {
            ConvertState convertState = commandStateService.getState(message.getChatId(), true);
            convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_TOO_MANY_FILES, locale));
            commandStateService.setState(message.getChatId(), convertState);
        } else if (message.hasText()) {
            ConvertState convertState = commandStateService.getState(message.getChatId(), true);
            Format targetFormat = checkTargetFormat(convertState.getFormat(), formatService.getAssociatedFormat(message.getText().trim()), locale);
            FileQueueItem queueItem = fileQueueService.add(message.getFrom(), convertState, targetFormat);
            String queuedMessage = queueMessageBuilder.getQueuedMessage(queueItem, convertState.getWarnings(), new Locale(convertState.getUserLanguage()));
            messageService.sendMessage(new SendMessageContext(message.getChatId(), queuedMessage)
                    .replyKeyboard(replyKeyboardService.getMainMenu(message.getChatId(), locale)));
            commandStateService.deleteState(message.getChatId());
        }
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId());
        Locale locale = userService.getLocale(message.getUser().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .replyKeyboard(replyKeyboardService.getMainMenu(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(long chatId) {
        return replyKeyboardService.getMainMenu(chatId, userService.getLocale((int) chatId));
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocale(message.getFrom().getId());
        messageService.sendMessage(
                new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                        .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale))

        );

        return true;
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());

        if (message.hasDocument()) {
            convertState.setFileId(message.getDocument().getFileId());
            convertState.setFileSize(message.getDocument().getFileSize());
            convertState.setFileName(message.getDocument().getFileName());
            convertState.setMimeType(message.getDocument().getMimeType());
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            convertState.setFormat(checkFormat(format, message.getDocument().getMimeType(), message.getDocument().getFileName(), message.getDocument().getFileId(), locale));
            if (convertState.getFormat() == Format.HTML && isBaseUrlMissed(message.getDocument().getFileId())) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_NO_BASE_URL_IN_HTML, locale));
            }
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            convertState.setFileId(photoSize.getFileId());
            convertState.setFileSize(photoSize.getFileSize());
            Format format = formatService.getImageFormat(photoSize.getFileId());
            checkFormat(format, null, null, photoSize.getFileId(), locale);
            convertState.setFormat(format);
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();
            convertState.setFileId(sticker.getFileId());
            convertState.setFileSize(sticker.getFileSize());
            convertState.setFormat(Format.WEBP);
        } else if (message.hasText()) {
            convertState.setFileId(message.getText());
            convertState.setFileSize(message.getText().length());
            convertState.setFormat(formatService.getFormat(message.getText()));
        }

        return convertState;
    }

    private boolean isBaseUrlMissed(String fileId) {
        File file = telegramService.downloadFileByFileId(fileId);
        try {
            Document parse = Jsoup.parse(file, StandardCharsets.UTF_8.name());
            Elements base = parse.head().getElementsByTag("base");

            return base == null || base.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void check(Message message, Locale locale) {
        if (message.hasDocument() || message.hasText() || message.hasPhoto()
                || message.hasSticker() && !message.getSticker().getAnimated()) {
            return;
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private Format checkFormat(Format format, String mimeType, String fileName, String fileId, Locale locale) {
        if (format != null) {
            return format;
        }

        if (StringUtils.isNotBlank(mimeType)) {
            LOGGER.debug("Format not resolved for " + mimeType + " and fileName " + fileName);
        } else {
            LOGGER.debug("Format not resolved for image " + fileId);
        }
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private Format checkTargetFormat(Format format, Format target, Locale locale) {
        if (target == null) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }
        if (Objects.equals(format, target)) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ANOTHER_FORMAT, locale));
        }
        boolean result = formatService.isConvertAvailable(format, target);
        if (result) {
            return target;
        }

        LOGGER.debug("Convert is not available for " + format + " to " + target);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }
}
