package ru.gadjini.any2any.model;

import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

public class EditMessageContext {

    private final long chatId;

    private final int messageId;

    private final String text;

    private InlineKeyboardMarkup replyKeyboard;

    public EditMessageContext(long chatId, int messageId, String text) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.text = text;
    }

    public long chatId() {
        return this.chatId;
    }

    public int messageId() {
        return this.messageId;
    }

    public String text() {
        return this.text;
    }

    public InlineKeyboardMarkup replyKeyboard() {
        return this.replyKeyboard;
    }

    public EditMessageContext replyKeyboard(final InlineKeyboardMarkup replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        return this;
    }

    public boolean hasKeyboard() {
        return replyKeyboard != null;
    }
}
