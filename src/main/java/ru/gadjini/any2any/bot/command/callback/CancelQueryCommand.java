package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.EditMessageContext;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.KeyboardCustomizer;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.filequeue.FileQueueBusinessService;

import java.util.Locale;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private FileQueueBusinessService fileQueueBusinessService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelQueryCommand(FileQueueBusinessService fileQueueBusinessService, MessageService messageService,
                              LocalisationService localisationService, UserService userService) {
        this.fileQueueBusinessService = fileQueueBusinessService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_QUERY_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        fileQueueBusinessService.cancel(queryItemId);
        Locale locale = userService.getLocale(callbackQuery.getFrom().getId());

        String actionFrom = requestParams.getString(Arg.ACTION_FROM.getKey());
        if (actionFrom.equals(CommandNames.QUERY_ITEM_DETAILS_COMMAND)) {
            messageService.editMessage(
                    new EditMessageContext(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale))
                            .replyKeyboard(new KeyboardCustomizer(callbackQuery.getMessage().getReplyMarkup()).removeExclude(CommandNames.GO_BACK_CALLBACK_COMMAND_NAME).getKeyboardMarkup())
            );
        } else {
            messageService.editMessage(
                    new EditMessageContext(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale)));
        }

        return null;
    }
}
