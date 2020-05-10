package ru.gadjini.any2any.filter;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface BotFilter {

    BotFilter setNext(BotFilter next);

    void doFilter(Update update);
}
