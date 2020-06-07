package ru.gadjini.any2any.model;

public class AnswerCallbackContext {

    private String queryId;

    private String text;

    public AnswerCallbackContext(String queryId, String text) {
        this.queryId = queryId;
        this.text = text;
    }

    public String queryId() {
        return this.queryId;
    }

    public String text() {
        return this.text;
    }
}
