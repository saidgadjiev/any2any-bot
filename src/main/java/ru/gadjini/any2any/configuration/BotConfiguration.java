package ru.gadjini.any2any.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;

@Configuration
public class BotConfiguration {

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, UserSynchronizedFilter userSynchronizedFilter,
                               StartCommandFilter startCommandFilter, TechWorkFilter techWorkFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               ChannelSubscriptionFilter subscriptionFilter, UpdatesHandlerFilter updatesHandler) {
        updateFilter.setNext(userSynchronizedFilter).setNext(activityFilter).setNext(mediaFilter)
                .setNext(startCommandFilter).setNext(subscriptionFilter)
                .setNext(techWorkFilter).setNext(updatesHandler);
        return updateFilter;
    }
}
