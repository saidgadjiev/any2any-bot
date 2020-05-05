package ru.gadjini.any2any.bot.command.convert;

import ru.gadjini.any2any.model.TgDocument;

public class ConvertState {

    private TgDocument document;

    private int messageId;

    public TgDocument getDocument() {
        return document;
    }

    public void setDocument(TgDocument document) {
        this.document = document;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
}
