package ru.gadjini.any2any.dao.command.keyboard;

import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;

public interface ReplyKeyboardDao {
    void store(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup);

    ReplyKeyboardMarkup get(long chatId);
}
