package ru.gadjini.any2any;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.property.ConversionProperties;
import ru.gadjini.any2any.property.DetectLanguageProperties;
import ru.gadjini.any2any.service.LocalisationService;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

@EnableConfigurationProperties(value = {
        ConversionProperties.class,
        DetectLanguageProperties.class
})
@EnableScheduling
@SpringBootApplication
public class Any2AnyApplication {

    private static final SmartLogger LOGGER = new SmartLogger(Any2AnyApplication.class);

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
