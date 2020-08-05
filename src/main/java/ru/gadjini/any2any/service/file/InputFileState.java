package ru.gadjini.any2any.service.file;

public class InputFileState {

    public static final String REPLY_TO_MESSAGE_ID = "replyToMessageId";

    public static final String STATE = "state";

    private Integer replyToMessageId;

    private State state = State.PENDING;

    public InputFileState(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public InputFileState() {}

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public enum State {

        PENDING,

        PROCESSING,

        COMPLETED
    }
}
