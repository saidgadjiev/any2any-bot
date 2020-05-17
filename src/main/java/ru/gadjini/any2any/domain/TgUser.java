package ru.gadjini.any2any.domain;

import java.util.Locale;

public class TgUser {

    public static final String TYPE = "tg_user";

    public static final String USER_ID = "user_id";

    public static final String USERNAME = "username";

    private int userId;

    private String username;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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
