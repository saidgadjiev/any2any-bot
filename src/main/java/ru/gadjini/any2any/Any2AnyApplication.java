package ru.gadjini.any2any;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

@ConfigurationPropertiesScan("ru.gadjini.telegram")
@EnableScheduling
@SpringBootApplication
@ComponentScan("ru")
public class Any2AnyApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(Any2AnyApplication.class);

    public static void main(String[] args) {
        setDefaultLocaleAndTZ();
        try {
            SpringApplication.run(Any2AnyApplication.class, args);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    private static void setDefaultLocaleAndTZ() {
        Locale.setDefault(new Locale(LocalisationService.EN_LOCALE));
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }
}
