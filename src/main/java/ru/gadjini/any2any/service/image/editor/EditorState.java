package ru.gadjini.any2any.service.image.editor;

public class EditorState {

    private String editFilePath;

    private String fileName;

    private int messageId;

    private String language;

    private Mode mode = Mode.NEGATIVE;

    private Screen screen = Screen.EDIT;

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

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public enum Mode {

        NEGATIVE,

        POSITIVE
    }

    public enum Screen {

        EDIT,

        COLOR
    }
}
