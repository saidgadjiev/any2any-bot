package ru.gadjini.any2any.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@ConfigurationProperties("proxy")
public class ProxyProperties {

    private String host;

    private int port;

    private String username;

    private String password;

    private DefaultBotOptions.ProxyType type = DefaultBotOptions.ProxyType.NO_PROXY;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public DefaultBotOptions.ProxyType getType() {
        return type;
    }

    public void setType(DefaultBotOptions.ProxyType type) {
        this.type = type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
