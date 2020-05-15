package ru.gadjini.any2any.service.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.any2any.service.converter.api.Format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public interface ReplyKeyboardService {
    ReplyKeyboardMarkup getMainMenu(long chatId, Locale locale);

    ReplyKeyboardMarkup goBack(long chatId, Locale locale);

    ReplyKeyboardMarkup getFormatsKeyboard(long chatId, Format format, Locale locale);

    ReplyKeyboardRemove removeKeyboard(long chatId);

    default KeyboardRow keyboardRow(String... buttons) {
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.addAll(Arrays.asList(buttons));

        return keyboardRow;
    }

    default ReplyKeyboardMarkup replyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setKeyboard(new ArrayList<>());
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }
}
