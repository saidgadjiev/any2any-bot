package ru.gadjini.any2any.bot.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.domain.TgUser;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.FileQueueService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;

import java.util.Locale;

@Component
public class StartCommand extends BotCommand {

    private UserService userService;

    private FileQueueService fileQueueService;

    private MessageService messageService;

    private LocalisationService localisationService;

    @Autowired
    public StartCommand(UserService userService, FileQueueService fileQueueService,
                        MessageService messageService, LocalisationService localisationService) {
        super(CommandNames.START_COMMAND, "");
        this.userService = userService;
        this.fileQueueService = fileQueueService;
        this.messageService = messageService;
        this.localisationService = localisationService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        TgUser tgUser = userService.save(user);
        messageService.sendMessage(new SendMessageContext(chat.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME, tgUser.getLocale())));
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        if (message.hasDocument()) {
            FileQueueItem queueItem = fileQueueService.add(message.getFrom(), message.getMessageId(), message.getDocument());
            sendQueuedMessage(queueItem);
        }
    }

    private void sendQueuedMessage(FileQueueItem queueItem) {
        Locale locale = userService.getLocale(queueItem.getUserId());
        String text = localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getPlaceInQueue()}, locale);
        messageService.sendMessage(new SendMessageContext(queueItem.getUserId(), text));
    }
}
