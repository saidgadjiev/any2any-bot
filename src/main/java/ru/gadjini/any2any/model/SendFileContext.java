package ru.gadjini.any2any.model;

import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.io.File;

public class SendFileContext {

    private long chatId;

    private File file;

    private String fileId;

    private Integer replyMessageId;

    private ReplyKeyboard replyKeyboard;

    private String caption;

    public SendFileContext(long chatId, File file) {
        this.chatId = chatId;
        this.file = file;
    }

    public SendFileContext(long chatId, String fileId) {
        this.chatId = chatId;
        this.fileId = fileId;
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

    public String fileId() {
        return fileId;
    }
}
