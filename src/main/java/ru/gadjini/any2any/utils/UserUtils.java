package ru.gadjini.any2any.utils;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;

public class UserUtils {

    private UserUtils() {}

    public static String name(User user) {
        StringBuilder fio = new StringBuilder();

        fio.append(user.getFirstName());

        if (StringUtils.isNotBlank(user.getLastName())) {
            fio.append(" ").append(user.getLastName());
        }

        return fio.toString();
    }

    public static String userLink(User user) {
        return userLink(user.getId(), name(user));
    }

    public static String userLink(int userId, String name) {
        StringBuilder link = new StringBuilder();

        link.append("<a href=\"tg://user?id=").append(userId).append("\">").append(name).append("</a>");

        return link.toString();
    }
}
