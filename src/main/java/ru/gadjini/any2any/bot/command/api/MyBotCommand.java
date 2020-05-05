package ru.gadjini.any2any.bot.command.api;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface MyBotCommand {

    default void processNonCommandUpdate(Message message, String text) {
    }

    default boolean accept(Message message) {
        return message.hasText();
    }

}
