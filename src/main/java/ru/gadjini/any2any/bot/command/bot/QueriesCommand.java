package ru.gadjini.any2any.bot.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.filequeue.FileQueueService;

import java.util.List;
import java.util.Locale;

@Component
public class QueriesCommand extends BotCommand {

    private FileQueueService fileQueueService;

    private LocalisationService localisationService;

    private UserService userService;

    private MessageService messageService;

    @Autowired
    public QueriesCommand(FileQueueService fileQueueService, LocalisationService localisationService, UserService userService, MessageService messageService) {
        super(CommandNames.QUERIES_COMMAND, "");
        this.fileQueueService = fileQueueService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        List<FileQueueItem> queries = fileQueueService.getActiveQueries(user.getId());
        messageService.sendMessage(
                new SendMessageContext(chat.getId(), message(user.getId(), queries))
        );
    }

    private String message(int userId, List<FileQueueItem> queueItems) {
        Locale locale = userService.getLocale(userId);
        StringBuilder message = new StringBuilder();
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_ACTIVE_QUERIES_HEADER, locale)).append("\n\n");
        int i = 1;
        for (FileQueueItem fileQueueItem : queueItems) {
            message.append(message(i++, fileQueueItem, locale)).append("\n");
        }

        return message.toString();
    }

    private String message(int number, FileQueueItem fileQueueItem, Locale locale) {
        StringBuilder message = new StringBuilder();
        message
                .append(number).append(") ").append(getFileName(fileQueueItem)).append("\n")
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_TARGET_FORMAT, new Object[]{fileQueueItem.getTargetFormat().name()}, locale))
                .append("\n");
        if (fileQueueItem.getStatus() == FileQueueItem.Status.WAITING) {
            message
                    .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUEUE_PLACE, new Object[]{fileQueueItem.getPlaceInQueue()}, locale))
                    .append("\n");
        }
        message
                .append(localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_STATUS, new Object[]{getStatus(fileQueueItem, locale)}, locale));

        return message.toString();
    }

    private String getFileName(FileQueueItem fileQueueItem) {
        switch (fileQueueItem.getFormat()) {
            case TEXT:
            case URL:
                if (fileQueueItem.getFileId().length() > 11) {
                    return fileQueueItem.getFileId().substring(0, 11) + "...";
                } else {
                    return fileQueueItem.getFileId();
                }
            default:
                return fileQueueItem.getFileName();
        }
    }

    private String getStatus(FileQueueItem fileQueueItem, Locale locale) {
        switch (fileQueueItem.getStatus()) {
            case WAITING:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_WAITING, locale);
            default:
                return localisationService.getMessage(MessagesProperties.MESSAGE_STATUS_PROCESSING, locale);
        }
    }
}
