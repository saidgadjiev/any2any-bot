package ru.gadjini.any2any.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.CommonConstants;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.model.bot.api.method.SendMessage;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.object.Update;
import ru.gadjini.any2any.model.bot.api.object.User;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class SubscriptionFilter extends BaseBotFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionFilter.class);

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public SubscriptionFilter(@Qualifier("limits") MessageService messageService, LocalisationService localisationService, UserService userService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void doFilter(Update update) {
        if (isSubscribedToTopsBot(TgMessage.getUserId(update))) {
            super.doFilter(update);
        } else {
            sendNeedSubscription(TgMessage.getUser(update));
        }
    }

    private boolean isSubscribedToTopsBot(int userId) {
        try {
            messageService.getChatMember(CommonConstants.TOP_BOTS_CHANNEL, userId);
        } catch (TelegramRequestException ex) {
            if (ex.getErrorCode() == 400) {
                if (ex.getResponse().contains("user not found")) {
                    return false;
                } else if (ex.getResponse().contains("chat not found")) {
                    LOGGER.error("Chat not found {}", CommonConstants.TOP_BOTS_CHANNEL);

                    return true;
                }
            }

            throw ex;
        }

        return true;
    }

    private void sendNeedSubscription(User user) {
        Locale locale = userService.getLocaleOrDefault(user.getId());
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEED_SUBSCRIPTION, locale);
        messageService.sendMessage(new SendMessage(user.getId(), msg));
    }
}
