package ru.gadjini.any2any.bot.command.convert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.ArrayList;
import java.util.List;

public class ConvertState {

    private String fileId;

    private String fileName;

    private String mimeType;

    private Integer fileSize;

    private int messageId;

    private Format format;

    private String userLanguage;

    @JsonIgnore
    private List<String> warnings = new ArrayList<>();

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void addWarn(String warn) {
        warnings.add(warn);
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
