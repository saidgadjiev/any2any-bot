package ru.gadjini.any2any.service.image.editor;

import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

public interface State {

    Name getName();

    default void update(ImageEditorCommand command, long chatId, String queryId) {

    }

    default void go(ImageEditorCommand command, long chatId, String queryId, Name name) {

    }

    default void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {

    }

    default void enter(ImageEditorCommand command, long chatId) {

    }

    default void transparentMode(ImageEditorCommand command, long chatId, String queryId, ModeState.Mode mode) {

    }

    default void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {

    }

    default void inaccuracy(ImageEditorCommand command, long chatId, String queryId, String inaccuracy) {

    }

    default void cancel(ImageEditorCommand command, long chatId, String queryId) {

    }

    default void userText(ImageEditorCommand command, long chatId, String text) {

    }

    default void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter effect) {

    }

    default void size(ImageEditorCommand command, long chatId, String queryId, String size) {

    }

    enum Filter {

        BLACK_AND_WHITE,

        SKETCH,

        NEGATIVE
    }

    enum Name {

        EDIT,

        TRANSPARENCY,

        COLOR,

        MODE,

        INACCURACY,

        FATHER,

        FILTERS,

        RESIZE
    }
}
