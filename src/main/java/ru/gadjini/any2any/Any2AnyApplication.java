package ru.gadjini.any2any;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.ApiContextInitializer;
import ru.gadjini.any2any.property.BotProperties;
import ru.gadjini.any2any.property.ConversionProperties;
import ru.gadjini.any2any.property.DetectLanguageProperties;
import ru.gadjini.any2any.property.ProxyProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;

import java.io.File;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

@EnableConfigurationProperties(value = {
        BotProperties.class,
        ProxyProperties.class,
        ConversionProperties.class,
        DetectLanguageProperties.class
})
@EnableScheduling
@SpringBootApplication
public class Any2AnyApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Any2AnyApplication.class);

    public static void main(String[] args) {
        setDefaultLocaleAndTZ();
        ApiContextInitializer.init();
        try {
            SpringApplication application = new SpringApplication(Any2AnyApplication.class);
            application.setApplicationContextClass(AnnotationConfigApplicationContext.class);

            application.run();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    private static void setDefaultLocaleAndTZ() {
        Locale.setDefault(new Locale(LocalisationService.EN_LOCALE));
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Autowired
    private TelegramService telegramService;

    @Override
    public void run(String... args) throws Exception {
        telegramService.downloadFileByFileId("BQACAgIAAxkBAAISt17n3d8W1UUOoIlnGnI0A21kiyvhAAIFCAAC4ZdAS1xPvgABuQ6H5BoE", new File("C:/test5352/testeeff/test.doc"));
    }
}
