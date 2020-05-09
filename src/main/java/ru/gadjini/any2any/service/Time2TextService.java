package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class Time2TextService {

    private LocalisationService localisationService;

    @Autowired
    public Time2TextService(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String time(Duration duration, Locale locale) {
        StringBuilder time = new StringBuilder();

        if (duration.toHoursPart() != 0) {
            time
                    .append(duration.toHoursPart()).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.HOUR_PART, locale));
        }
        if (time.length() > 0) {
            time.append(" ");
        }
        if (duration.toMinutesPart() != 0) {
            time
                    .append(duration.toMinutesPart()).append(" ")
                    .append(localisationService.getMessage(MessagesProperties.MINUTE_PART, locale));
        }

        return time.toString();
    }

    public static void main(String[] args) {
        Duration duration = Duration.of(30, ChronoUnit.SECONDS);

        System.out.println(duration.toHoursPart());
        System.out.println(duration.toMinutesPart());

    }
}
