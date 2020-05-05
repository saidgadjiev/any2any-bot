package ru.gadjini.any2any.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bot")
public class BotProperties {

    private String name;

    private String token;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
