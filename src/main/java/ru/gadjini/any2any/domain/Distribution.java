package ru.gadjini.any2any.domain;

import ru.gadjini.any2any.service.LocalisationService;

public class Distribution {

    public static final String USER_ID = "user_id";

    public static final String MESSAGE_RU = "message_ru";

    public static final String MESSAGE_EN = "message_en";

    private int userId;

    private TgUser user;

    private String messageRu;

    private String messageEn;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public TgUser getUser() {
        return user;
    }

    public void setUser(TgUser user) {
        this.user = user;
    }

    public String getMessageRu() {
        return messageRu;
    }

    public void setMessageRu(String messageRu) {
        this.messageRu = messageRu;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public String getLocalisedMessage() {
        if (user.getLanguageCode().equals(LocalisationService.RU_LOCALE)) {
            return messageRu;
        }

        return messageEn;
    }
}
