package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import ru.gadjini.any2any.filter.*;
import ru.gadjini.any2any.property.ProxyProperties;

@Configuration
public class BotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfiguration.class);

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DefaultBotOptions botOptions(ProxyProperties proxyProperties) {
        DefaultBotOptions defaultBotOptions = ApiContext.getInstance(DefaultBotOptions.class);

        if (proxyProperties.getType() != DefaultBotOptions.ProxyType.NO_PROXY) {
            defaultBotOptions.setProxyType(proxyProperties.getType());
            defaultBotOptions.setProxyHost(proxyProperties.getHost());
            defaultBotOptions.setProxyPort(proxyProperties.getPort());

            LOGGER.debug("Proxy type: {} host: {} port: {}", proxyProperties.getType(), proxyProperties.getHost(), proxyProperties.getPort());
        }

        return defaultBotOptions;
    }

    @Bean
    public BotFilter botFilter(Any2AnyBotFilter any2AnyBotFilter, SubscriptionFilter subscriptionFilter,
                               UpdateFilter updateFilter, StartCommandFilter startCommandFilter) {
        updateFilter.setNext(startCommandFilter).setNext(subscriptionFilter).setNext(any2AnyBotFilter);
        return updateFilter;
    }
}
