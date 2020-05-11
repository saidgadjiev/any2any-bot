package ru.gadjini.any2any.model;

import java.io.File;

public class SendFileContext {

    private long chatId;

    private File file;

    private Integer replyMessageId;

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


}
