package ru.gadjini.any2any.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.smart.bot.commons.filter.*;
import ru.gadjini.telegram.smart.bot.commons.filter.subscription.ChannelSubscriptionFilter;

@Configuration
public class BotConfiguration {

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               MediaFilter mediaFilter, LastActivityFilter activityFilter,
                               ChannelSubscriptionFilter subscriptionFilter, UpdatesHandlerFilter updatesHandler) {
        updateFilter.setNext(mediaFilter).setNext(startCommandFilter).setNext(subscriptionFilter)
                .setNext(activityFilter).setNext(updatesHandler);
        return updateFilter;
    }
}
