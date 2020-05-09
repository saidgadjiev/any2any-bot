package ru.gadjini.any2any.event;

public class QueueItemCanceled {

    private int id;

    public QueueItemCanceled(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
