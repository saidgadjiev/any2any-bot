package ru.gadjini.any2any.utils;

import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.StringUtils;
import ru.gadjini.any2any.service.text.TextDirection;

public class TextUtils {

    private TextUtils() {

    }

    public static String removeAllEmojis(String str, TextDirection direction) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        String s = EmojiParser.removeAllEmojis(str);

        if (direction == TextDirection.RL) {
            s = brainFuckOnRlEmojis(s);
        }
        return replaceBrainFuckNumbers(s);
    }

    //Wtf moment with rl text
    private static String brainFuckOnRlEmojis(String str) {
        return str.replace(" ️", "");
    }

    private static String replaceBrainFuckNumbers(String str) {
        return str.replace("❽", "8").replace("❾", "9");
    }
}