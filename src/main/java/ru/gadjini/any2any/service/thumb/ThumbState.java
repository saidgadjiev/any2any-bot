package ru.gadjini.any2any.service.thumb;

import ru.gadjini.any2any.model.Any2AnyFile;

public class ThumbState {

    private Any2AnyFile file;

    private Any2AnyFile thumb;

    private int replyToMessageId;

    public Any2AnyFile getFile() {
        return file;
    }

    public void setFile(Any2AnyFile file) {
        this.file = file;
    }

    public Any2AnyFile getThumb() {
        return thumb;
    }

    public void setThumb(Any2AnyFile thumb) {
        this.thumb = thumb;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }
}
