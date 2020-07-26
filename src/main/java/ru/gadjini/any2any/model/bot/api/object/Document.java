package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Document {

    private static final String FILEID_FIELD = "file_id";
    private static final String FILENAME_FIELD = "file_name";
    private static final String MIMETYPE_FIELD = "mime_type";
    private static final String FILESIZE_FIELD = "file_size";
    private static final String THUMBNAIL = "thumb";

    @JsonProperty(FILEID_FIELD)
    private String fileId;
    @JsonProperty(FILENAME_FIELD)
    private String fileName;
    @JsonProperty(MIMETYPE_FIELD)
    private String mimeType;
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize;
    @JsonProperty(THUMBNAIL)
    private Thumb thumb;

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public Thumb getThumb() {
        return thumb;
    }

    public boolean hasThumb() {
        return thumb != null;
    }

    @Override
    public String toString() {
        return "Document{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
