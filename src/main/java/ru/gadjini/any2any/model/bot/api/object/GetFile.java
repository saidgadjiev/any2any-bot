package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetFile {

    public static final String METHOD = "downloadfile";

    private static final String FILE_ID = "file_id";

    @JsonProperty(FILE_ID)
    private String fileId;

    private String path;

    public GetFile() {}

    public GetFile(String fileId, String path) {
        this.fileId = fileId;
        this.path = path;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
