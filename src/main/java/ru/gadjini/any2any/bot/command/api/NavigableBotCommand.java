package ru.gadjini.any2any.bot.command.api;

import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;

public interface NavigableBotCommand extends MyBotCommand {

    String getParentCommandName(long chatId);

    String getHistoryName();

    default void restore(TgMessage message) {

    }

    default ReplyKeyboardMarkup getKeyboard(long chatId) {
        throw new UnsupportedOperationException();
    }

    default void leave(long chatId) {

    }

    default boolean setPrevCommand(long chatId, String prevCommand) {
        return false;
    }

    default boolean canLeave(long chatId) {
        return true;
    }
}
