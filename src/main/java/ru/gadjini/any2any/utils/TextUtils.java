package ru.gadjini.any2any.utils;

import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.StringUtils;

public class TextUtils {

    private TextUtils() {

    }

    public static String removeAllEmojis(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        String s = EmojiParser.removeAllEmojis(str);

        return replaceBrainFuckNumbers(brainFuckOnRlEmojis(s));
    }

    //Wtf moment with rl text
    private static String brainFuckOnRlEmojis(String str) {
        return str.replace(" ️", "");
    }

    private static String replaceBrainFuckNumbers(String str) {
        return str.replace("❽", "8").replace("❾", "9");
    }
}
