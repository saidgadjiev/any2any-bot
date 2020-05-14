package ru.gadjini.any2any.bot.command.api;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.model.TgMessage;

public interface NavigableBotCommand extends MyBotCommand {

    String getHistoryName();

    default void restore(TgMessage message) {

    }

    default ReplyKeyboardMarkup getKeyboard(long chatId) {
        throw new UnsupportedOperationException();
    }

    default void leave(long chatId) {

    }
}
