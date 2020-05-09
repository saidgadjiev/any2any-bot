package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.command.navigator.CallbackCommandNavigator;

@Component
public class GoBackCallbackCommand implements CallbackBotCommand {

    private CallbackCommandNavigator callbackCommandNavigator;

    @Autowired
    public void setCallbackCommandNavigator(CallbackCommandNavigator callbackCommandNavigator) {
        this.callbackCommandNavigator = callbackCommandNavigator;
    }

    @Override
    public String getName() {
        return CommandNames.GO_BACK_CALLBACK_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        String prevCommandName = requestParams.getString(Arg.PREV_HISTORY_NAME.getKey());

        callbackCommandNavigator.popTo(TgMessage.from(callbackQuery), prevCommandName, requestParams);

        return null;
    }
}
