package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

public class EditorState {

    private String currentFilePath;

    private String prevFilePath;

    private String fileId;

    private String fileName;

    private int messageId;

    private String language;

    private String inaccuracy = "10.0";

    private ModeState.Mode mode = ModeState.Mode.NEGATIVE;

    private State.Name stateName = State.Name.EDIT;

    public String getImage() {
        return currentFilePath;
    }

    public void setImage(String image) {
        this.currentFilePath = image;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public void setCurrentFilePath(String currentFilePath) {
        this.currentFilePath = currentFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ModeState.Mode getMode() {
        return mode;
    }

    public void setMode(ModeState.Mode mode) {
        this.mode = mode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public State.Name getStateName() {
        return stateName;
    }

    public void setStateName(State.Name stateName) {
        this.stateName = stateName;
    }

    public String getInaccuracy() {
        return inaccuracy;
    }

    public void setInaccuracy(String inaccuracy) {
        this.inaccuracy = inaccuracy;
    }

    public String getPrevFilePath() {
        return prevFilePath;
    }

    public void setPrevFilePath(String prevFilePath) {
        this.prevFilePath = prevFilePath;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public boolean canCancel() {
        return StringUtils.isNotBlank(prevFilePath);
    }
}
