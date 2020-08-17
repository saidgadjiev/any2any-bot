package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueMessageBuilder;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import java.util.Locale;

@Component
public class QueryItemDetailsCommand implements CallbackBotCommand {

    private ConversionQueueService fileQueueService;

    private ConversionQueueMessageBuilder messageBuilder;

    private UserService userService;

    private MessageService messageService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public QueryItemDetailsCommand(ConversionQueueService fileQueueService, ConversionQueueMessageBuilder messageBuilder,
                                   UserService userService, @Qualifier("messagelimits") MessageService messageService,
                                   InlineKeyboardService inlineKeyboardService) {
        this.fileQueueService = fileQueueService;
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.messageService = messageService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public String getName() {
        return CommandNames.QUERY_ITEM_DETAILS_COMMAND;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
        ConversionQueueItem item = fileQueueService.getItem(queryItemId);
        if (item == null) {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), messageBuilder.queryItemNotFound(locale))
            );
        } else {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), messageBuilder.getItem(item, locale))
                            .setReplyMarkup(inlineKeyboardService.getQueryDetailsKeyboard(queryItemId, locale))
            );
        }
    }
}
