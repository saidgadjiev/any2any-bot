package ru.gadjini.any2any.service.image.editor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.keyboard.ImageEditorCommand;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

import java.util.Locale;

@Service
public class StateFatherProxy implements State {

    private StateFather stateFather;

    @Autowired
    public void setStateFather(StateFather stateFather) {
        this.stateFather = stateFather;
    }

    @Override
    public Name getName() {
        return stateFather.getName();
    }

    @Override
    public void applyFilter(ImageEditorCommand command, long chatId, String queryId, Filter filter) {
        withProcessing(() -> stateFather.applyFilter(command, chatId, queryId, filter));
    }

    @Override
    public void size(ImageEditorCommand command, long chatId, String queryId, String size) {
        withProcessing(() -> stateFather.size(command, chatId, queryId, size));
    }

    @Override
    public void go(ImageEditorCommand command, long chatId, String queryId, Name name) {
        withProcessing(() -> stateFather.go(command, chatId, queryId, name));
    }

    @Override
    public void goBack(ImageEditorCommand command, CallbackQuery callbackQuery) {
        stateFather.goBack(command, callbackQuery);
    }

    @Override
    public void transparentMode(ImageEditorCommand command, long chatId, String queryId, ModeState.Mode mode) {
        withProcessing(() -> stateFather.transparentMode(command, chatId, queryId, mode));
    }

    @Override
    public void transparentColor(ImageEditorCommand command, long chatId, String queryId, String text) {
        withProcessing(() -> stateFather.transparentColor(command, chatId, queryId, text));
    }

    @Override
    public void inaccuracy(ImageEditorCommand command, long chatId, String queryId, String inaccuracy) {
        withProcessing(() -> stateFather.inaccuracy(command, chatId, queryId, inaccuracy));
    }

    @Override
    public void cancel(ImageEditorCommand command, long chatId, String queryId) {
        withProcessing(() -> stateFather.cancel(command, chatId, queryId));
    }

    @Override
    public void userText(ImageEditorCommand command, long chatId, String text) {
        stateFather.userText(command, chatId, text);
    }

    public void initializeState(ImageEditorCommand command, long chatId, Any2AnyFile any2AnyFile, Locale locale) {
        stateFather.initializeState(command, chatId, any2AnyFile, locale);
    }

    public void leave(ImageEditorCommand command, long chatId) {
        stateFather.leave(command, chatId);
    }

    //TODO: тут должна была быть защита от кучи запросов от пользователя
    private void withProcessing(Runnable runnable) {
        runnable.run();
    }
}
