package ru.gadjini.any2any.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.image.editor.transparency.Color;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory) {
        this.buttonFactory = buttonFactory;
    }

    public InlineKeyboardMarkup getFilesListKeyboard(Set<Integer> filesIds) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        int i = 1;
        List<List<Integer>> lists = Lists.partition(new ArrayList<>(filesIds), 4);
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int id : list) {
                row.add(buttonFactory.extractFileButton(String.valueOf(i++), id));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getColorsKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        List<Color> colors = Arrays.stream(Color.values()).collect(Collectors.toList());
        List<List<Color>> lists = Lists.partition(colors, 5);

        for (List<Color> l : lists) {
            if (l.size() == 5) {
                List<InlineKeyboardButton> buttons = new ArrayList<>();

                l.forEach(color -> buttons.add(buttonFactory.delegateButton(
                        "color." + color.name().toLowerCase(),
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                        new RequestParams().add(Arg.TRANSPARENT_COLOR.getKey(), color.name().toLowerCase()),
                        locale
                )));
                inlineKeyboardMarkup.getKeyboard().add(buttons);
            } else {
                List<InlineKeyboardButton> buttons = inlineKeyboardMarkup.getKeyboard().get(inlineKeyboardMarkup.getKeyboard().size() - 1);

                l.forEach(color -> buttons.add(buttonFactory.delegateButton(
                        "color." + color.name().toLowerCase(),
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME,
                        new RequestParams().add(Arg.TRANSPARENT_COLOR.getKey(), color.name().toLowerCase()),
                        locale
                )));
            }
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.moreColorsButton(locale)));
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.imageColorButton(locale)));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.updateButton(locale)));

            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
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

    public InlineKeyboardMarkup getResizeKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("16x16",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "16x16")),
                buttonFactory.delegateButton("32x32",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "32x32")),
                buttonFactory.delegateButton("64x64",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "64x64"))));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("480x360",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "480x360")),
                buttonFactory.delegateButton("640x480",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "640x480")),
                buttonFactory.delegateButton("1280x720",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "1280x720"))));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.updateButton(locale)));

            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getImageFiltersKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.blackAndWhiteFilterButton(locale),
                buttonFactory.sketchFilterButton(locale)
        ));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.negativeButton(locale)
        ));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.updateButton(locale)));

            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getImageEditKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.transparencyButton(locale)
        ));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.filtersButton(locale),
                buttonFactory.resizeButton(locale)
        ));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelButton(locale), buttonFactory.updateButton(locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale))
            );
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getTransparencyKeyboard(Locale locale) {
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

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale
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
