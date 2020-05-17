package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.any2any.dao.UserDao;
import ru.gadjini.any2any.domain.CreateOrUpdateResult;
import ru.gadjini.any2any.domain.TgUser;

import java.util.Locale;

@Service
public class UserService {

    private UserDao userDao;

    private LocalisationService localisationService;

    @Autowired
    public UserService(UserDao userDao, LocalisationService localisationService) {
        this.userDao = userDao;
        this.localisationService = localisationService;
    }

    public CreateOrUpdateResult createOrUpdate(User user) {
        TgUser tgUser = new TgUser();
        tgUser.setUserId(user.getId());
        tgUser.setUsername(user.getUserName());

        String language = localisationService.getSupportedLocales().stream()
                .filter(locale -> locale.getLanguage().equals(user.getLanguageCode()))
                .findAny().orElse(Locale.getDefault()).getLanguage();
        tgUser.setLocale(language);
        String state = userDao.createOrUpdate(tgUser);

        return new CreateOrUpdateResult(tgUser, CreateOrUpdateResult.State.fromDesc(state));
    }

    public Locale getLocaleOrDefault(int userId) {
        String locale = userDao.getLocale(userId);

        if (StringUtils.isNotBlank(locale)) {
            return new Locale(locale);
        }

        return Locale.getDefault();
    }

    public void changeLocale(int userId, Locale locale) {
        userDao.updateLocale(userId, locale);
    }
}
