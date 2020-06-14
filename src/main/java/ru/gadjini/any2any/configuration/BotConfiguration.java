package ru.gadjini.any2any.configuration;

import org.apache.commons.lang3.StringUtils;
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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static java.net.Authenticator.getDefault;
import static java.net.Authenticator.setDefault;

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

            if (StringUtils.isNotBlank(proxyProperties.getUsername())) {
                setPasswordAuthenticator(proxyProperties);
            }

            LOGGER.debug("Proxy type: {} host: {} port: {}", proxyProperties.getType(), proxyProperties.getHost(), proxyProperties.getPort());
        }

        return defaultBotOptions;
    }

    @Bean
    public BotFilter botFilter(Any2AnyBotFilter any2AnyBotFilter,
                               UpdateFilter updateFilter, StartCommandFilter startCommandFilter,
                               TelegramLimitsFilter telegramLimitsFilter, LastActivityFilter activityFilter,
                               DistributionFilter distributionFilter) {
        updateFilter.setNext(telegramLimitsFilter).setNext(startCommandFilter).setNext(activityFilter)
                .setNext(distributionFilter).setNext(any2AnyBotFilter);
        return updateFilter;
    }

    private void setPasswordAuthenticator(ProxyProperties proxyProperties) {
        final Authenticator old = getDefault();
        setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost().equals(proxyProperties.getHost()) && getRequestingPort() == proxyProperties.getPort()) {
                    return new PasswordAuthentication(proxyProperties.getUsername(), proxyProperties.getPassword().toCharArray());
                } else {
                    return old.requestPasswordAuthenticationInstance(getRequestingHost(), getRequestingSite(), getRequestingPort(), getRequestingProtocol(), getRequestingPrompt(), getRequestingScheme(), getRequestingURL(), getRequestorType());
                }
            }
        });
    }
}
