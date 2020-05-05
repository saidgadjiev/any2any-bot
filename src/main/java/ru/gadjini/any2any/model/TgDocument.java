package ru.gadjini.any2any.model;

import org.telegram.telegrambots.meta.api.objects.Document;

public class TgDocument {

    private String fileId;

    private String fileName;

    private String mimeType;

    private Integer fileSize;

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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public static TgDocument from(Document document) {
        TgDocument tgDocument = new TgDocument();

        tgDocument.setFileId(document.getFileId());
        tgDocument.setFileName(document.getFileName());
        tgDocument.setFileSize(document.getFileSize());
        tgDocument.setMimeType(document.getMimeType());

        return tgDocument;
    }
}
