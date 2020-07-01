package ru.gadjini.any2any.filter;

import org.springframework.stereotype.Component;
import ru.gadjini.any2any.model.bot.api.object.Update;

@Component
public class UpdateFilter extends BaseBotFilter {

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            super.doFilter(update);
        }
    }
}
