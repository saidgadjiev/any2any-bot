package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Video {

    private static final String FILEID_FIELD = "file_id";
    private static final String MIMETYPE_FIELD = "mime_type";
    private static final String FILESIZE_FIELD = "file_size";

    @JsonProperty(FILEID_FIELD)
    private String fileId;
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType;
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize;

    public String getFileId() {
        return fileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return "Video{" +
                "fileId='" + fileId + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
