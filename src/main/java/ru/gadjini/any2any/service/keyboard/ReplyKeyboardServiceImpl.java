package ru.gadjini.any2any.service.keyboard;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.OcrService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.impl.FormatService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements ReplyKeyboardService {

    private FormatService formatMapService;

    private LocalisationService localisationService;

    @Autowired
    public ReplyKeyboardServiceImpl(FormatService formatMapService, LocalisationService localisationService) {
        this.formatMapService = formatMapService;
        this.localisationService = localisationService;
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
    public ReplyKeyboardMarkup getOcrKeyboard(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();
        List<String> languages = new ArrayList<>();
        for (Locale l : OcrService.SUPPORTED_LOCALES) {
            languages.add(StringUtils.capitalize(l.getDisplayLanguage(locale)));
        }
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(languages.toArray(new String[0])));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup getMainMenu(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.QUERIES_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.RENAME_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.EXTRACT_TEXT_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.UNZIP_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.ARCHIVE_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_NAME, locale), localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_NAME, locale)));
        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.HELP_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup goBack(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup getFormatsKeyboard(long chatId, Format format, Locale locale) {
        List<Format> targetFormats = new ArrayList<>(formatMapService.getTargetFormats(format));
        targetFormats.sort(Comparator.comparing(Enum::name));
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkup();

        List<KeyboardRow> keyboard = replyKeyboardMarkup.getKeyboard();
        List<List<Format>> lists = Lists.partition(targetFormats, 3);
        for (List<Format> list : lists) {
            keyboard.add(keyboardRow(list.stream().map(Enum::name).toArray(String[]::new)));
        }
        keyboard.add(keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        return new ReplyKeyboardRemove();
    }

}
