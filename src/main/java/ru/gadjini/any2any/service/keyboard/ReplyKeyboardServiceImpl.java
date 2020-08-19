package ru.gadjini.any2any.service.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements ReplyKeyboardService {

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public ReplyKeyboardServiceImpl(LocalisationService localisationService, UserService userService) {
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public ReplyKeyboardMarkup getAdminKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        if (userService.isAdmin((int) chatId)) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.DOWNLOAD_FILE_COMMAND_NAME, locale)));
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.EXECUTE_CONVERSION_COMMAND_NAME, locale)));
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.REMOVE_GARBAGE_FILES_COMMAND_NAME, locale)));
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.RESET_FILE_LIMIT_COMMAND_NAME, locale)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup archiveTypesKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(Format.ZIP.name(), Format.RAR.name()));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<String> languages = new ArrayList<>();
        for (Locale l : localisationService.getSupportedLocales()) {
            languages.add(StringUtils.capitalize(l.getDisplayLanguage(l)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(languages.toArray(new String[0])));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup getMainMenu(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.IMAGE_EDITOR_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.QUERIES_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.RENAME_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.EXTRACT_TEXT_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.UNZIP_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.ARCHIVE_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.HELP_COMMAND_NAME, locale)));
        if (userService.isAdmin((int) chatId)) {
            replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.ADMIN_COMMAND_NAME, locale)));
        }

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup goBack(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup cancel(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        return new ReplyKeyboardRemove();
    }

}
