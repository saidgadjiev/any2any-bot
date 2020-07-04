package ru.gadjini.any2any.domain;

import ru.gadjini.any2any.service.conversion.api.Format;

public class UnzipQueueItem {

    public static final String NAME = "unzip_queue";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String FILE = "file";

    public static final String TYPE = "type";

    public static final String STATUS = "status";

    private int id;

    private int userId;

    private TgFile file;

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

    public TgFile getFile() {
        return file;
    }

    public void setFile(TgFile file) {
        this.file = file;
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
