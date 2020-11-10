package ru.gadjini.any2any.request;

import ru.gadjini.telegram.smart.bot.commons.command.impl.CallbackDelegate;

public enum Arg {

    CALLBACK_DELEGATE(CallbackDelegate.ARG_NAME),
    TRANSPARENT_MODE("e"),
    TRANSPARENT_COLOR("f"),
    GO_BACK("g"),
    EDIT_STATE_NAME("k"),
    IMAGE_FILTER("m"),
    INACCURACY("l"),
    IMAGE_SIZE("o"),
    UPDATE_EDITED_IMAGE("p");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
