package ru.gadjini.any2any.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.any2any.common.CommandNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory) {
        this.buttonFactory = buttonFactory;
    }

    public InlineKeyboardMarkup getQueryDetailsKeyboard(int queryItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelQueryItem(queryItemId, locale)));
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.goBackCallbackButton(CommandNames.QUERIES_COMMAND, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getQueriesKeyboard(List<Integer> queryItemsIds) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        int i = 1;
        List<List<Integer>> lists = Lists.partition(queryItemsIds, 4);
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int queryItemId : list) {
                row.add(buttonFactory.queryItemDetails(String.valueOf(i++), queryItemId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup inlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        inlineKeyboardMarkup.setKeyboard(new ArrayList<>());

        return inlineKeyboardMarkup;
    }
}
