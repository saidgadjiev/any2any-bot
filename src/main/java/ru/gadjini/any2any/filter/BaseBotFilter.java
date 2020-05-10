package ru.gadjini.any2any.filter;

import org.telegram.telegrambots.meta.api.objects.Update;

public class BaseBotFilter implements BotFilter {

    private BotFilter next;

    @Override
    public final BotFilter setNext(BotFilter next) {
        this.next = next;

        return next;
    }

    @Override
    public void doFilter(Update update) {
        if (next != null) {
            next.doFilter(update);
        }
    }
}
