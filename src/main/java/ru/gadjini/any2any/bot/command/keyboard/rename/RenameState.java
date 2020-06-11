package ru.gadjini.any2any.bot.command.keyboard.rename;

import ru.gadjini.any2any.model.Any2AnyFile;

public class RenameState {

   private Any2AnyFile file;

    private String language;

    private int replyMessageId;

    public void setFile(Any2AnyFile file) {
        this.file = file;
    }

    public Any2AnyFile getFile() {
        return file;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getReplyMessageId() {
        return replyMessageId;
    }

    public void setReplyMessageId(int replyMessageId) {
        this.replyMessageId = replyMessageId;
    }
}
