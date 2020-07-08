package ru.gadjini.any2any.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.UserDao;
import ru.gadjini.any2any.domain.CreateOrUpdateResult;
import ru.gadjini.any2any.domain.TgUser;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.model.bot.api.object.User;

import java.util.Locale;

@Service
public class UserService {

    private static final SmartLogger LOGGER = new SmartLogger(UserService.class);

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
        tgUser.setOriginalLocale(user.getLanguageCode());

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

    public void activity(User user) {
        if (user == null) {
            LOGGER.error("User is null");
            return;
        }
        int updated = userDao.updateActivity(user.getId());

        if (updated == 0) {
            createOrUpdate(user);
            LOGGER.debug("User created", user.getId());
        }
    }

    public void blockUser(int userId) {
        userDao.blockUser(userId);
    }

    public boolean deadlock(Throwable ex) {
        if (ex instanceof TelegramRequestException) {
            TelegramRequestException exception = (TelegramRequestException) ex;
            if (exception.getErrorCode() == 403) {
                blockUser((int) exception.getChatId());

                return true;
            }
        }

        return false;
    }

    public void changeLocale(int userId, Locale locale) {
        userDao.updateLocale(userId, locale);
    }
}
