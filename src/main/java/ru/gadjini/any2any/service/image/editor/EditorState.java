package ru.gadjini.any2any.service.image.editor;

public class EditorState {

    private String editFilePath;

    private String fileName;

    private int messageId;

    private String language;

    private Mode mode = Mode.REMOVE;

    public String getImage() {
        return editFilePath;
    }

    public void setImage(String image) {
        this.editFilePath = image;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getEditFilePath() {
        return editFilePath;
    }

    public void setEditFilePath(String editFilePath) {
        this.editFilePath = editFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public enum Mode {

        REMOVE,

        EXCLUDE
    }
}
