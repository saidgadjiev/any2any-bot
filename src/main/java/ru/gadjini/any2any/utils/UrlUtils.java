package ru.gadjini.any2any.utils;

import org.apache.commons.lang3.StringUtils;

public class UrlUtils {

    private UrlUtils() {}

    public static boolean isUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www.");
    }

    public static boolean hasScheme(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        return url.startsWith("http://") || url.startsWith("https://");
    }
}
