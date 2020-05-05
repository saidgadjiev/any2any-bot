package ru.gadjini.any2any.bot.command.api;

import ru.gadjini.any2any.model.TgMessage;

public interface NavigableBotCommand extends MyBotCommand {

    String getHistoryName();

    default void restore(TgMessage message) {

    }

    default void leave(long chatId) {

    }
}
