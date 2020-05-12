package ru.gadjini.any2any.domain;

public class FileReport {

    public static final String TYPE = "file_report";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String QUEUE_ITEM_ID = "queue_item_id";

    private int id;

    private int userId;

    private int queueItemId;

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

    public int getQueueItemId() {
        return queueItemId;
    }

    public void setQueueItemId(int queueItemId) {
        this.queueItemId = queueItemId;
    }
}
