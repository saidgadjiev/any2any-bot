package ru.gadjini.any2any.model.bot.api.method.send;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendPhoto {

    public static final String METHOD = "sendphoto";

    public static final String CHATID_FIELD = "chat_id";

    public static final String PHOTO = "photo";

    @JsonProperty(CHATID_FIELD)
    private String chatId;

    @JsonProperty(PHOTO)
    private String photo;

    public SendPhoto(long chatId, String photo) {
        this.chatId = String.valueOf(chatId);
        this.photo = photo;
    }

    public String getChatId() {
        return chatId;
    }

    public String getPhoto() {
        return photo;
    }
}
