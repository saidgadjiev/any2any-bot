package ru.gadjini.any2any.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.UserService;

@Component
public class LastActivityFilter extends BaseBotFilter {

    private UserService userService;

    @Autowired
    public LastActivityFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void doFilter(Update update) {
        userService.activity(TgMessage.getUser(update));
        super.doFilter(update);
    }
}
