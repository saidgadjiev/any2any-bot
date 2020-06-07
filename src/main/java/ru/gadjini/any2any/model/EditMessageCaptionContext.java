package ru.gadjini.any2any.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class EditMessageCaptionContext {

    private final long chatId;

    private final int messageId;

    private String caption;

    private InlineKeyboardMarkup replyKeyboard;

    public EditMessageCaptionContext(long chatId, int messageId, String caption) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.caption = caption;
    }

    public long chatId() {
        return this.chatId;
    }

    public int messageId() {
        return this.messageId;
    }

    public String caption() {
        return this.caption;
    }

    public InlineKeyboardMarkup replyKeyboard() {
        return this.replyKeyboard;
    }

    public EditMessageCaptionContext replyKeyboard(final InlineKeyboardMarkup replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        return this;
    }

    public boolean hasKeyboard() {
        return replyKeyboard != null;
    }
}
