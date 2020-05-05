package ru.gadjini.any2any.model;

import org.telegram.telegrambots.meta.api.objects.Document;

import java.io.File;

public class SendDocumentContext {

    private long chatId;

    private File file;

    private Integer replyMessageId;

    public SendDocumentContext(long chatId, File file) {
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

    public SendDocumentContext replyMessageId(final Integer replyMessageId) {
        this.replyMessageId = replyMessageId;
        return this;
    }


}
