package ru.gadjini.any2any.service.image.editor;

import org.apache.commons.lang3.StringUtils;

public class EditorState {

    private String editFilePath;

    private String prevEditFilePath;

    private String fileName;

    private int messageId;

    private String language;

    private String inaccuracy = "10.0";

    private ModeState.Mode mode = ModeState.Mode.NEGATIVE;

    private State.Name stateName = State.Name.EDIT;

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

    public String getPrevEditFilePath() {
        return prevEditFilePath;
    }

    public void setPrevEditFilePath(String prevEditFilePath) {
        this.prevEditFilePath = prevEditFilePath;
    }

    public boolean canCancel() {
        return StringUtils.isNotBlank(prevEditFilePath);
    }
}
