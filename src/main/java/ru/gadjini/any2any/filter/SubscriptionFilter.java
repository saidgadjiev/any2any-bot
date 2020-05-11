package ru.gadjini.any2any.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.any2any.common.CommonConstants;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TelegramMethodException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;

import java.util.Locale;

@Component
public class SubscriptionFilter extends BaseBotFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionFilter.class);

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public SubscriptionFilter(MessageService messageService, LocalisationService localisationService, UserService userService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void doFilter(Update update) {
        if (isSubscribedToTopsBot(TgMessage.getUserId(update))) {
            super.doFilter(update);
        } else {
            sendNeedSubscription(TgMessage.getUserId(update));
        }
    }

    private boolean isSubscribedToTopsBot(int userId) {
        try {
            messageService.getChatMember(CommonConstants.TOP_BOTS_CHANNEL, userId);
        } catch (TelegramMethodException ex) {
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

    private void sendNeedSubscription(int userId) {
        Locale locale = userService.getLocale(userId);
        messageService.sendMessage(new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_NEED_SUBSCRIPTION, locale)));
    }
}
