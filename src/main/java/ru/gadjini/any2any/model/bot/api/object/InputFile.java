package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InputFile {

    public static final String MEDIA_FILE_ID_FIELD = "file_id";
    public static final String MEDIA_FILE_PATH_FIELD = "file_path";
    public static final String MEDIA_FILE_NAME_FIELD = "file_name";

    @JsonProperty(MEDIA_FILE_ID_FIELD)
    private String fileId;
    @JsonProperty(MEDIA_FILE_PATH_FIELD)
    private String filePath;
    @JsonProperty(MEDIA_FILE_NAME_FIELD)
    private String fileName;

    public InputFile() {
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
