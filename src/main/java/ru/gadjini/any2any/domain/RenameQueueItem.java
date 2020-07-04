package ru.gadjini.any2any.domain;

public class RenameQueueItem {

    public static final String TYPE = "rename_queue";

    public static final String ID = "id";

    public static final String FILE_ID = "file_id";

    public static final String MIME_TYPE = "mime_type";

    public static final String FILE_NAME = "file_name";

    public static final String NEW_FILE_NAME = "new_file_name";

    public static final String USER_ID = "user_id";

    public static final String REPLY_TO_MESSAGE_ID = "reply_to_message_id";

    private int id;

    private String fileId;

    private String mimeType;

    private String fileName;

    private String newFileName;

    private int userId;

    private int replyToMessageId;

    private Status status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {


        WAITING(0),

        PROCESSING(1);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown queue item status " + code);
        }
    }
}
