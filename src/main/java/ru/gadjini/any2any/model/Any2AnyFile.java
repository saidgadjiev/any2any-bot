package ru.gadjini.any2any.model;

import ru.gadjini.any2any.service.converter.api.Format;

public class Any2AnyFile {

    private String fileId;

    private Format format;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }
}
