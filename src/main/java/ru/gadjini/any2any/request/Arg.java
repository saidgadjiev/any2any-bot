package ru.gadjini.any2any.request;

public enum Arg {

    QUEUE_ITEM_ID("a"),
    PREV_HISTORY_NAME("b"),
    ACTION_FROM("c"),
    CALLBACK_DELEGATE("d"),
    TRANSPARENT_MODE("e"),
    TRANSPARENT_COLOR("f"),
    IMAGE_EDITOR_SCREEN("g"),
    INACCURACY("h");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
