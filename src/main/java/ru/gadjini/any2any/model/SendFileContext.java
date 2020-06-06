package ru.gadjini.any2any.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.io.File;

public class SendFileContext {

    private long chatId;

    private File file;

    private Integer replyMessageId;

    private ReplyKeyboard replyKeyboard;

    private String caption;

    public SendFileContext(long chatId, File file) {
        this.chatId = chatId;
        this.file = file;
    }

    public long chatId() {
        return chatId;
    }

    public File file() {
        return file;
    }

    public Integer replyMessageId() {
        return this.replyMessageId;
    }

    public SendFileContext replyMessageId(final Integer replyMessageId) {
        this.replyMessageId = replyMessageId;
        return this;
    }

    public ReplyKeyboard replyKeyboard() {
        return this.replyKeyboard;
    }

    public SendFileContext replyKeyboard(final ReplyKeyboard replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        return this;
    }

    public String caption() {
        return this.caption;
    }

    public SendFileContext caption(final String caption) {
        this.caption = caption;
        return this;
    }
}
