package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InputMedia {

    public static final String MEDIA_FILE_ID_FIELD = "media_file_id";
    public static final String MEDIA_FILE_PATH_FIELD = "media_file_path";
    public static final String MEDIA_FILE_CAPTION = "media_file_caption";

    @JsonProperty(MEDIA_FILE_ID_FIELD)
    private String fileId;
    @JsonProperty(MEDIA_FILE_PATH_FIELD)
    private String filePath;
    @JsonProperty(MEDIA_FILE_CAPTION)
    private String caption;

    public InputMedia() {}

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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
