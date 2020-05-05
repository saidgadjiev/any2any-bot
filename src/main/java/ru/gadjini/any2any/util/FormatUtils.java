package ru.gadjini.any2any.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import ru.gadjini.any2any.service.converter.api.Format;

public class FormatUtils {

    private FormatUtils() {

    }

    public static Format getFormat(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension)) {
            extension = extension.substring(1);
        }

        extension = FilenameUtils.getExtension(fileName);

        return Format.valueOf(extension.toUpperCase());
    }
}
