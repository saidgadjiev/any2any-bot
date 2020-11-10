package ru.gadjini.any2any.service.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public interface Any2AnyReplyKeyboardService extends ReplyKeyboardService {

    ReplyKeyboardMarkup getAdminKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup archiveTypesKeyboard(long chatId, Locale locale);

    ReplyKeyboardMarkup goBack(long chatId, Locale locale);

    ReplyKeyboardMarkup cancel(long chatId, Locale locale);

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
