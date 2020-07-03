package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableCallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueMessageBuilder;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QueriesCommand implements KeyboardBotCommand, NavigableCallbackBotCommand, BotCommand {

    private Set<String> names = new HashSet<>();

    private ConversionQueueService fileQueueService;

    private MessageService messageService;

    private UserService userService;

    private InlineKeyboardService inlineKeyboardService;

    private ConversionQueueMessageBuilder messageBuilder;

    @Autowired
    public QueriesCommand(ConversionQueueService fileQueueService, LocalisationService localisationService,
                          @Qualifier("limits") MessageService messageService, UserService userService, InlineKeyboardService inlineKeyboardService,
                          ConversionQueueMessageBuilder messageBuilder) {
        this.fileQueueService = fileQueueService;
        this.userService = userService;
        this.messageBuilder = messageBuilder;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            names.add(localisationService.getMessage(MessagesProperties.QUERIES_COMMAND_NAME, locale));
        }
    }

    @Override
    public String getName() {
        return CommandNames.QUERIES_COMMAND;
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public void processMessage(Message message) {
        List<ConversionQueueItem> queries = fileQueueService.getActiveItems(message.getFromUser().getId());
        messageService.sendMessage(
                new HtmlMessage(message.getChat().getId(), messageBuilder.getItems(queries, userService.getLocaleOrDefault(message.getFromUser().getId())))
                        .setReplyMarkup(inlineKeyboardService.getQueriesKeyboard(queries.stream().map(ConversionQueueItem::getId).collect(Collectors.toList())))
        );
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.QUERIES_COMMAND;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        List<ConversionQueueItem> queries = fileQueueService.getActiveItems(message.getFromUser().getId());
        messageService.sendMessage(
                new HtmlMessage((long) message.getFromUser().getId(), messageBuilder.getItems(queries, userService.getLocaleOrDefault(message.getFromUser().getId())))
                        .setReplyMarkup(inlineKeyboardService.getQueriesKeyboard(queries.stream().map(ConversionQueueItem::getId).collect(Collectors.toList())))
        );

        return false;
    }

    @Override
    public void restore(TgMessage tgMessage, ReplyKeyboard replyKeyboard, RequestParams requestParams) {
        List<ConversionQueueItem> queries = fileQueueService.getActiveItems(tgMessage.getUser().getId());
        messageService.editMessage(
                new EditMessageText(tgMessage.getUser().getId(), tgMessage.getMessageId(), messageBuilder.getItems(queries, userService.getLocaleOrDefault(tgMessage.getUser().getId())))
                        .setReplyMarkup(inlineKeyboardService.getQueriesKeyboard(queries.stream().map(ConversionQueueItem::getId).collect(Collectors.toList())))
        );
    }
}
