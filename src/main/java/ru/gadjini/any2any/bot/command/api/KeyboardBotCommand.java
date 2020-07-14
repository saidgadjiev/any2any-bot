package ru.gadjini.any2any.bot.command.api;

import ru.gadjini.any2any.model.bot.api.object.Message;

public interface KeyboardBotCommand extends MyBotCommand {

    boolean canHandle(long chatId, String command);
    
    default boolean isTextCommand() {
        return false;
    }

    boolean processMessage(Message message, String text);
}
