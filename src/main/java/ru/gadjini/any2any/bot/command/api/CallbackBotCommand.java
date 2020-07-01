package ru.gadjini.any2any.bot.command.api;

import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.RequestParams;

public interface CallbackBotCommand extends MyBotCommand {

    String getName();

    /**
     * @return an answer message code or null if command doesn't have an answer
     */
    String processMessage(CallbackQuery callbackQuery, RequestParams requestParams);

    default void processNonCommandCallback(CallbackQuery callbackQuery, RequestParams requestParams) {

    }
}
