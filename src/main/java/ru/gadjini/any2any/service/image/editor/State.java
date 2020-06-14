package ru.gadjini.any2any.service.image.editor;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

public interface State {

    Name getName();

    default void go(ImageEditorCommand command, long chatId, Name name) {

    }

    default void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {

    }

    default void enter(ImageEditorCommand command, long chatId) {

    }

    default void transparentMode(ImageEditorCommand command, long chatId, ModeState.Mode mode) {

    }

    default void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {

    }

    default void inaccuracy(ImageEditorCommand command, long chatId, String inaccuracy) {

    }

    default void cancel(ImageEditorCommand command, long chatId, String queryId) {

    }

    default void userText(ImageEditorCommand command, long chatId, String text) {

    }

    default void applyEffect(ImageEditorCommand command, long chatId, String queryId, Effect effect) {

    }

    enum Effect {

        BLACK_AND_WHITE,

        SKETCH
    }

    enum Name {

        EDIT,

        TRANSPARENCY,

        COLOR,

        MODE,

        INACCURACY,

        FATHER,

        EFFECTS
    }
}
