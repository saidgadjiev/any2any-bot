package ru.gadjini.any2any.service.converter.api;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.utils.MimeTypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FormatService {

    private final Map<List<Format>, List<Format>> formats = Map.ofEntries(
            Map.entry(List.of(Format.DOC), List.of(Format.DOCX, Format.TXT, Format.PDF, Format.EPUB, Format.RTF, Format.TIFF)),
            Map.entry(List.of(Format.DOCX), List.of(Format.DOC, Format.TXT, Format.PDF, Format.EPUB, Format.RTF, Format.TIFF)),
            Map.entry(List.of(Format.PNG, Format.DEVICE_PHOTO), List.of(Format.PDF, Format.JPG, Format.BMP, Format.WEBP, Format.STICKER)),
            Map.entry(List.of(Format.STICKER, Format.WEBP), List.of(Format.PNG, Format.JPG, Format.PDF)),
            Map.entry(List.of(Format.JPG, Format.JPEG), List.of(Format.PDF, Format.PNG, Format.BMP, Format.WEBP, Format.STICKER)),
            Map.entry(List.of(Format.SVG), List.of(Format.PDF, Format.PNG, Format.JPG, Format.BMP, Format.WEBP, Format.STICKER)),
            Map.entry(List.of(Format.BMP), List.of(Format.PDF, Format.PNG, Format.JPG, Format.WEBP, Format.STICKER)),
            Map.entry(List.of(Format.TIFF), List.of(Format.DOC, Format.DOCX, Format.PDF)),
            Map.entry(List.of(Format.URL, Format.HTML), List.of(Format.PDF)),
            Map.entry(List.of(Format.TXT, Format.TEXT), List.of(Format.PDF, Format.DOC, Format.DOCX)),
            Map.entry(List.of(Format.EPUB), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.RTF)),
            Map.entry(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX, Format.EPUB, Format.TIFF))
    );

    public Map<List<Format>, List<Format>> getFormats() {
        return formats;
    }

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<List<Format>, List<Format>> entry : formats.entrySet()) {
            if (entry.getKey().contains(srcFormat)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }

    public Format getFormat(String text) {
        if (isUrl(text)) {
            return Format.URL;
        }

        return Format.TEXT;
    }

    public Format getFormat(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension)) {
            extension = extension.substring(1);
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }

        return Format.valueOf(extension.toUpperCase());
    }

    public boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
    }

    private boolean isUrl(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        return text.startsWith("http://") || text.startsWith("https://") || text.startsWith("www.");
    }
}
