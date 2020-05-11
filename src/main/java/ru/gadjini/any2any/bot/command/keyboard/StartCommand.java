package ru.gadjini.any2any.bot.command.keyboard;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.domain.TgUser;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.filequeue.FileQueueService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StartCommand extends BotCommand implements KeyboardBotCommand, NavigableBotCommand {

    private final Map<Integer, ConvertState> states = new ConcurrentHashMap<>();

    private UserService userService;

    private FileQueueService fileQueueService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private FormatService formatService;

    private TelegramService telegramService;

    @Autowired
    public StartCommand(UserService userService, FileQueueService fileQueueService,
                        MessageService messageService, LocalisationService localisationService,
                        ReplyKeyboardService replyKeyboardService, FormatService formatService,
                        TelegramService telegramService) {
        super(CommandNames.START_COMMAND, "");
        this.userService = userService;
        this.fileQueueService = fileQueueService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.formatService = formatService;
        this.telegramService = telegramService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        TgUser tgUser = userService.save(user);
        messageService.sendMessage(
                new SendMessageContext(chat.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME, tgUser.getLocale()))
                        .replyKeyboard(replyKeyboardService.getMainMenu(tgUser.getLocale()))
        );
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocale(message.getFrom().getId());

        if (!states.containsKey(message.getFrom().getId())) {
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    new SendMessageContext(message.getChatId(), getMessage(convertState.getWarnings(), locale))
                            .replyKeyboard(replyKeyboardService.getKeyboard(convertState.getFormat(), locale))
            );
            states.put(message.getFrom().getId(), convertState);
        } else if (message.hasText()) {
            ConvertState convertState = states.get(message.getFrom().getId());
            FileQueueItem queueItem = fileQueueService.add(message.getFrom(), convertState, Format.valueOf(message.getText().toUpperCase()));
            sendQueuedMessage(queueItem, new Locale(convertState.getUserLanguage()));
            states.remove(message.getFrom().getId());
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return false;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        return false;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        states.remove(message.getUser().getId());
        Locale locale = userService.getLocale(message.getUser().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .replyKeyboard(replyKeyboardService.getMainMenu(locale)));
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument() || message.hasText() || message.hasPhoto();
    }

    private void sendQueuedMessage(FileQueueItem queueItem, Locale locale) {
        String text = localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getPlaceInQueue()}, locale);
        messageService.sendMessage(new SendMessageContext(queueItem.getUserId(), text).replyKeyboard(replyKeyboardService.getMainMenu(locale)));
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
            convertState.setFormat(formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType()));
            if (convertState.getFormat() == Format.HTML && isBaseUrlMissed(message.getDocument().getFileId())) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_NO_BASE_URL_IN_HTML, locale));
            }
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            convertState.setFileId(photoSize.getFileId());
            convertState.setFileSize(photoSize.getFileSize());
            convertState.setFormat(Format.DEVICE_PHOTO);
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

    private String getMessage(List<String> warns, Locale locale) {
        StringBuilder warnsText = new StringBuilder();
        int i = 1;
        for (String warn : warns) {
            if (warnsText.length() > 0) {
                warnsText.append("\n");
            }
            warnsText.append(i++).append(" )").append(warn);
        }

        StringBuilder message = new StringBuilder();
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION, locale));
        if (warns.size() > 0) {
            message
                    .append("\n\n")
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_WARNINGS, new Object[]{warnsText.toString()}, locale));
        }

        return message.toString();
    }
}
