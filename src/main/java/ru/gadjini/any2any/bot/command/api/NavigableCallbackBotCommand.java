package ru.gadjini.any2any.bot.command.api;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.request.RequestParams;

public interface NavigableCallbackBotCommand extends MyBotCommand {

    String getName();

    default void restore(TgMessage tgMessage, ReplyKeyboard replyKeyboard, RequestParams requestParams) {

    }

    default void leave(long chatId) {

    }

    default boolean isAcquireKeyboard() {
        return false;
    }
}
