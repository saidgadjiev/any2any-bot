package ru.gadjini.any2any.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wkhtmltopdf")
public class WkhtmltopdfProperties {

    private String execution;

    public String getExecution() {
        return execution;
    }

    public void setExecution(String execution) {
        this.execution = execution;
    }
}
