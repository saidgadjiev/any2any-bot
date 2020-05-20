package ru.gadjini.any2any.domain;

import java.util.Locale;

public class TgUser {

    public static final String TYPE = "tg_user";

    public static final String USER_ID = "user_id";

    public static final String USERNAME ="username";

    public static final String LOCALE = "locale";

    private int userId;

    private String username;

    private String locale;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLanguageCode() {
        return locale;
    }

    public Locale getLocale() {
        return Locale.getDefault();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
