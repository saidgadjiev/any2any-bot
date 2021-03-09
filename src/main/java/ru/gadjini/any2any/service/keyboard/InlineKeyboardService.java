package ru.gadjini.any2any.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.service.image.editor.State;
import ru.gadjini.any2any.service.image.editor.transparency.Color;
import ru.gadjini.any2any.service.image.editor.transparency.ModeState;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory, SmartInlineKeyboardService smartInlineKeyboardService) {
        this.buttonFactory = buttonFactory;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
    }

    public InlineKeyboardMarkup getColorsKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        List<Color> colors = Arrays.stream(Color.values()).collect(Collectors.toList());
        List<List<Color>> lists = Lists.partition(colors, 5);

        for (List<Color> l : lists) {
            if (l.size() == 5) {
                List<InlineKeyboardButton> buttons = new ArrayList<>();

                l.forEach(color -> buttons.add(buttonFactory.delegateButton(
                        "color." + color.name().toLowerCase(),
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
                        new RequestParams().add(Arg.TRANSPARENT_COLOR.getKey(), color.name().toLowerCase()),
                        locale
                )));
                inlineKeyboardMarkup.getKeyboard().add(buttons);
            } else {
                List<InlineKeyboardButton> buttons = inlineKeyboardMarkup.getKeyboard().get(inlineKeyboardMarkup.getKeyboard().size() - 1);

                l.forEach(color -> buttons.add(buttonFactory.delegateButton(
                        "color." + color.name().toLowerCase(),
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME,
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
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getTransparentModeKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_NEGATIVE_MODE,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.TRANSPARENT_MODE.getKey(), ModeState.Mode.NEGATIVE.name()), locale),
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_POSITIVE_MODE,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.TRANSPARENT_MODE.getKey(), ModeState.Mode.POSITIVE.name()), locale)));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getInaccuracyKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("5.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "5")),
                buttonFactory.delegateButton("10.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "10")),
                buttonFactory.delegateButton("15.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "15"))));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("20.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "20")),
                buttonFactory.delegateButton("25.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "25")),
                buttonFactory.delegateButton("30.0%",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.INACCURACY.getKey(), "30"))));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale
                )));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getResizeKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("16x16",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "16x16")),
                buttonFactory.delegateButton("32x32",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "32x32")),
                buttonFactory.delegateButton("64x64",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "64x64"))));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("480x360",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "480x360")),
                buttonFactory.delegateButton("512x512",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "640x480")),
                buttonFactory.delegateButton("640x480",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "640x480")),
                buttonFactory.delegateButton("1280x720",
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "1280x720"))));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.updateButton(locale)));

            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getImageFiltersKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

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
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getImageEditKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

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
        InlineKeyboardMarkup inlineKeyboardMarkup = smartInlineKeyboardService.inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.TRANSPARENT_MODE_COMMAND_DESCRIPTION,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.MODE.name()), locale),
                buttonFactory.delegateButton(MessagesProperties.MESSAGE_IMAGE_EDITOR_CHOOSE_COLOR,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.COLOR.name()), locale)));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.TRANSPARENT_INACCURACY_COMMAND_DESCRIPTION,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.EDIT_STATE_NAME.getKey(), State.Name.INACCURACY.name()), locale
                )));

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                        FileUtilsCommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale
                )));

        return inlineKeyboardMarkup;
    }
}
