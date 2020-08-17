package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.KeyboardCustomizer;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.ConvertionService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private ConvertionService convertionService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelQueryCommand(ConvertionService convertionService, @Qualifier("messagelimits") MessageService messageService,
                              LocalisationService localisationService, UserService userService) {
        this.convertionService = convertionService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_QUERY_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        convertionService.cancel(queryItemId);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());

        String actionFrom = requestParams.getString(Arg.ACTION_FROM.getKey());
        if (actionFrom.equals(CommandNames.QUERY_ITEM_DETAILS_COMMAND)) {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale))
                            .setReplyMarkup(new KeyboardCustomizer(callbackQuery.getMessage().getReplyMarkup()).removeExclude(CommandNames.GO_BACK_CALLBACK_COMMAND_NAME).getKeyboardMarkup())
            );
        } else {
            messageService.editMessage(
                    new EditMessageText(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale)));
        }
    }
}
