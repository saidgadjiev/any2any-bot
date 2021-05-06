package ru.gadjini.any2any.service.keyboard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.Redis;
import ru.gadjini.telegram.smart.bot.commons.dao.command.keyboard.ReplyKeyboardDao;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardHolderService;

import java.util.Collections;
import java.util.Locale;

@Service
@KeyboardHolder
public class CurrReplyKeyboard implements Any2AnyReplyKeyboardService, ReplyKeyboardHolderService {

    private ReplyKeyboardDao replyKeyboardDao;

    private Any2AnyReplyKeyboardService keyboardService;

    public CurrReplyKeyboard(@Redis ReplyKeyboardDao replyKeyboardDao,
                             @Qualifier("keyboard") Any2AnyReplyKeyboardService keyboardService) {
        this.replyKeyboardDao = replyKeyboardDao;
        this.keyboardService = keyboardService;
    }

    @Override
    public ReplyKeyboardMarkup getAdminKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.getAdminKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup archiveTypesKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.archiveTypesKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.smartFileFeatureKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.languageKeyboard(chatId, locale));
    }

    public ReplyKeyboardMarkup mainMenuKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, (ReplyKeyboardMarkup) keyboardService.mainMenuKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup goBack(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.goBack(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup cancel(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.cancel(chatId, locale));
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        ReplyKeyboardRemove replyKeyboardRemove = keyboardService.removeKeyboard(chatId);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(Collections.emptyList());
        setCurrentKeyboard(chatId, replyKeyboardMarkup);

        return replyKeyboardRemove;
    }

    @Override
    public ReplyKeyboardMarkup getCurrentReplyKeyboard(long chatId) {
        return replyKeyboardDao.get(chatId);
    }

    private ReplyKeyboardMarkup setCurrentKeyboard(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        replyKeyboardDao.store(chatId, replyKeyboardMarkup);

        return replyKeyboardMarkup;
    }
}
