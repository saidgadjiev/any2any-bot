package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
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
import ru.gadjini.any2any.model.TgDocument;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.FileQueueService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.util.FormatUtils;

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

    @Autowired
    public StartCommand(UserService userService, FileQueueService fileQueueService,
                        MessageService messageService, LocalisationService localisationService,
                        ReplyKeyboardService replyKeyboardService) {
        super(CommandNames.START_COMMAND, "");
        this.userService = userService;
        this.fileQueueService = fileQueueService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        TgUser tgUser = userService.save(user);
        messageService.sendMessage(new SendMessageContext(chat.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME, tgUser.getLocale())));
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        ConvertState convertState = states.get(message.getFrom().getId());

        if (convertState == null && message.hasDocument()) {
            convertState = new ConvertState();
            convertState.setDocument(TgDocument.from(message.getDocument()));
            convertState.setMessageId(message.getMessageId());
            states.put(message.getFrom().getId(), convertState);
            Locale locale = userService.getLocale(message.getFrom().getId());
            messageService.sendMessage(
                    new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_TARGET_EXTENSION, locale))
                            .replyKeyboard(replyKeyboardService.getKeyboard(FormatUtils.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType()), locale))
            );
        } else if (convertState != null && message.hasText()) {
            FileQueueItem queueItem = fileQueueService.add(message.getFrom(), convertState.getMessageId(), convertState.getDocument(), Format.valueOf(message.getText().toUpperCase()));
            sendQueuedMessage(queueItem);
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
        Locale locale = userService.getLocale(message.getUser().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .replyKeyboard(replyKeyboardService.removeKeyboard()));
    }

    @Override
    public void leave(long chatId) {
        states.remove((int) chatId);
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument() || message.hasText();
    }

    private void sendQueuedMessage(FileQueueItem queueItem) {
        Locale locale = userService.getLocale(queueItem.getUserId());
        String text = localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getPlaceInQueue()}, locale);
        messageService.sendMessage(new SendMessageContext(queueItem.getUserId(), text).replyKeyboard(replyKeyboardService.removeKeyboard()));
    }
}
