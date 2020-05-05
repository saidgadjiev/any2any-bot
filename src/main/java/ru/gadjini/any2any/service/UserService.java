package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gadjini.any2any.dao.UserDao;
import ru.gadjini.any2any.domain.TgUser;

import java.util.Locale;

@Service
public class UserService {

    private UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public TgUser save(User user) {
        TgUser tgUser = new TgUser();
        tgUser.setUserId(user.getId());
        userDao.save(tgUser);

        return tgUser;
    }

    public Locale getLocale(int userId) {
        return Locale.getDefault();
    }
}
