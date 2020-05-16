package ru.gadjini.any2any.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public class SendMessageContext {

    private final long chatId;

    private final String text;

    private Integer replyMessageId;

    private ReplyKeyboard replyKeyboard;

    private boolean html = true;

    private boolean webPagePreview = false;

    public SendMessageContext(long chatId, String text) {
        this.chatId = chatId;
        this.text = text;
    }

    public long chatId() {
        return this.chatId;
    }

    public String text() {
        return this.text;
    }

    public ReplyKeyboard replyKeyboard() {
        return this.replyKeyboard;
    }

    public SendMessageContext replyKeyboard(final ReplyKeyboard replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        return this;
    }

    public boolean hasKeyboard() {
        return replyKeyboard != null;
    }

    public boolean html() {
        return this.html;
    }

    public SendMessageContext html(final boolean html) {
        this.html = html;
        return this;
    }

    public boolean webPagePreview() {
        return this.webPagePreview;
    }

    public SendMessageContext webPagePreview(final boolean webPagePreview) {
        this.webPagePreview = webPagePreview;
        return this;
    }

    public SendMessageContext replyMessageId(Integer replyMessageId) {
        this.replyMessageId = replyMessageId;

        return this;
    }

    public Integer replyMessageId() {
        return replyMessageId;
    }
}
