package ru.gadjini.any2any.filter;

import ru.gadjini.any2any.model.bot.api.object.Update;

public interface BotFilter {

    BotFilter setNext(BotFilter next);

    void doFilter(Update update);
}
