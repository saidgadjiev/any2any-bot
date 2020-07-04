package ru.gadjini.any2any.domain;

import ru.gadjini.any2any.service.converter.api.Format;

import java.util.List;

public class ArchiveQueueItem {

    public static final String NAME = "archive_queue";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String FILES = "files";

    public static final String TYPE = "type";

    public static final String STATUS = "status";

    private int id;

    private int userId;

    private List<TgFile> files;

    private Format type;

    private Status status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<TgFile> getFiles() {
        return files;
    }

    public void setFiles(List<TgFile> files) {
        this.files = files;
    }

    public Format getType() {
        return type;
    }

    public void setType(Format type) {
        this.type = type;
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
    }
}
