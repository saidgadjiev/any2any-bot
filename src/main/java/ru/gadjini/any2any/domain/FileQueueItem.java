package ru.gadjini.any2any.domain;

import ru.gadjini.any2any.service.converter.api.Format;

import java.time.ZonedDateTime;

public class FileQueueItem {

    public static final String TYPE = "queue";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String MESSAGE_ID = "message_id";

    public static final String FILE_ID = "file_id";

    public static final String FILE_NAME = "file_name";

    public static final String FORMAT = "format";

    public static final String SIZE = "size";

    public static final String CREATED_AT = "created_at";

    public static final String TARGET_FORMAT = "target_format";

    public static final String PLACE_IN_QUEUE = "place_in_queue";

    private int id;

    private ZonedDateTime createdAt;

    private int userId;

    private int messageId;

    private String fileId;

    private String fileName;

    private Format format;

    private int size;

    private int placeInQueue;

    private Format targetFormat;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
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

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPlaceInQueue() {
        return placeInQueue;
    }

    public void setPlaceInQueue(int placeInQueue) {
        this.placeInQueue = placeInQueue;
    }

    public Format getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Format targetFormat) {
        this.targetFormat = targetFormat;
    }
}
