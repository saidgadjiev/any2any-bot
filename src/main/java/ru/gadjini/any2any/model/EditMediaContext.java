package ru.gadjini.any2any.model;

import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;

import java.io.File;

public class EditMediaContext {

    private final long chatId;

    private final int messageId;

    private String caption;

    private File file;

    private String fileId;

    private InlineKeyboardMarkup replyKeyboard;

    public EditMediaContext(long chatId, int messageId, File file) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.file = file;
    }

    public EditMediaContext(long chatId, int messageId, String fileId) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.fileId = fileId;
    }

    public long chatId() {
        return this.chatId;
    }

    public int messageId() {
        return this.messageId;
    }

    public File file() {
        return file;
    }

    public String caption() {
        return this.caption;
    }

    public EditMediaContext caption(final String caption) {
        this.caption = caption;
        return this;
    }

    public InlineKeyboardMarkup replyKeyboard() {
        return this.replyKeyboard;
    }

    public EditMediaContext replyKeyboard(final InlineKeyboardMarkup replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        return this;
    }

    public boolean hasKeyboard() {
        return replyKeyboard != null;
    }

    public String fileId() {
        return fileId;
    }
}
