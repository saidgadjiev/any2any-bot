package ru.gadjini.any2any.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import ru.gadjini.any2any.filter.*;

@Configuration
public class BotConfiguration implements Jackson2ObjectMapperBuilderCustomizer {

    @Bean
    public BotFilter botFilter(Any2AnyBotFilter any2AnyBotFilter,
                               UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               TelegramLimitsFilter telegramLimitsFilter, LastActivityFilter activityFilter,
                               DistributionFilter distributionFilter) {
        updateFilter.setNext(telegramLimitsFilter).setNext(startCommandFilter).setNext(activityFilter)
                .setNext(distributionFilter).setNext(any2AnyBotFilter);
        return updateFilter;
    }

    @Override
    public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
        jacksonObjectMapperBuilder.visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
}
