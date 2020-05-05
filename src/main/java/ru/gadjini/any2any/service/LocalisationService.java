package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

@Service
public class LocalisationService {

    private static final String RU_LOCALE = "ru";

    private MessageSource messageSource;

    @Autowired
    public LocalisationService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String messageCode, @Nonnull Locale locale) {
        return getMessage(messageCode, null, locale);
    }

    public String getMessage(String messageCode, Object[] args, @Nonnull Locale locale) {
        return messageSource.getMessage(messageCode, args, locale);
    }

    public List<Locale> getSupportedLocales() {
        return List.of(new Locale(RU_LOCALE));
    }
}
