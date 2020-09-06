package ru.gadjini.any2any.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.service.Any2AnyBotService;
import ru.gadjini.telegram.smart.bot.commons.filter.BaseBotFilter;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Update;

@Component
public class Any2AnyBotFilter extends BaseBotFilter {

    private Any2AnyBotService any2AnyBotService;

    @Autowired
    public Any2AnyBotFilter(Any2AnyBotService any2AnyBotService) {
        this.any2AnyBotService = any2AnyBotService;
    }

    @Override
    public void doFilter(Update update) {
        any2AnyBotService.onUpdateReceived(update);
    }
}
