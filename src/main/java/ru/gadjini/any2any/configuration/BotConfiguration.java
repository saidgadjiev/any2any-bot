package ru.gadjini.any2any.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.any2any.filter.*;

@Configuration
public class BotConfiguration {

    @Bean
    public BotFilter botFilter(Any2AnyBotFilter any2AnyBotFilter,
                               UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               TelegramLimitsFilter telegramLimitsFilter, LastActivityFilter activityFilter,
                               DistributionFilter distributionFilter) {
        updateFilter.setNext(telegramLimitsFilter).setNext(startCommandFilter).setNext(activityFilter)
                .setNext(distributionFilter).setNext(any2AnyBotFilter);
        return updateFilter;
    }
}
