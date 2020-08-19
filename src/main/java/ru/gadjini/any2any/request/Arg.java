package ru.gadjini.any2any.request;

public enum Arg {

    PREV_HISTORY_NAME("b"),
    CALLBACK_DELEGATE("d"),
    TRANSPARENT_MODE("e"),
    TRANSPARENT_COLOR("f"),
    GO_BACK("g"),
    EDIT_STATE_NAME("k"),
    IMAGE_FILTER("m"),
    INACCURACY("l"),
    IMAGE_SIZE("o"),
    UPDATE_EDITED_IMAGE("p"),
    EXTRACT_FILE_ID("r"),
    JOB_ID("s"),
    OFFSET("t"),
    PAGINATION("u"),
    PREV_LIMIT("v");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
