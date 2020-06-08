package ru.gadjini.any2any.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.image.editor.Color;
import ru.gadjini.any2any.service.image.editor.ModeState;
import ru.gadjini.any2any.service.image.editor.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory) {
        this.buttonFactory = buttonFactory;
    }

    public InlineKeyboardMarkup getColorsKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        List<Color> colors = Arrays.stream(Color.values()).collect(Collectors.toList());
        List<List<Color>> lists = Lists.partition(colors, 5);

        for (List<Color> l : lists) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();

            l.forEach(color -> buttons.add(buttonFactory.delegateButton(
                    "color." + color.name().toLowerCase(),
                    CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                    new RequestParams().add(Arg.TRANSPARENT_COLOR.getKey(), color.name().toLowerCase()),
                    locale
            )));
            inlineKeyboardMarkup.getKeyboard().add(buttons);
        }
        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getTransparentModeKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_NEGATIVE_MODE,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.TRANSPARENT_MODE.getKey(), ModeState.Mode.NEGATIVE.name()), locale),
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_POSITIVE_MODE,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.TRANSPARENT_MODE.getKey(), ModeState.Mode.POSITIVE.name()), locale)));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getInaccuracyKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("5.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "5")),
                buttonFactory.delegateButton("10.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "10")),
                buttonFactory.delegateButton("15.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "15"))));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("20.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "20")),
                buttonFactory.delegateButton("25.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "25")),
                buttonFactory.delegateButton("30.0%",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "30"))));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale
                )));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getEditImageTransparentKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.TRANSPARENT_MODE_COMMAND_DESCRIPTION,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.MODE.name()), locale),
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_CHOOSE_COLOR,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.COLOR.name()), locale)));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.TRANSPARENT_INACCURACY_COMMAND_DESCRIPTION,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.INACCURACY.name()), locale
                )));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getQueryDetailsKeyboard(int queryItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelQueryItem(queryItemId, CommandNames.QUERY_ITEM_DETAILS_COMMAND, locale)));
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.goBackCallbackButton(CommandNames.QUERIES_COMMAND, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup cancelQuery(int queryItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelQueryItem(queryItemId, CommandNames.START_COMMAND, locale)));

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

    public InlineKeyboardMarkup reportKeyboard(int queueItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.report(queueItemId, locale)));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup inlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        inlineKeyboardMarkup.setKeyboard(new ArrayList<>());

        return inlineKeyboardMarkup;
    }
}
