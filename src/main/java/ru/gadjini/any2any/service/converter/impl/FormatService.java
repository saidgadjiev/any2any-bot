package ru.gadjini.any2any.service.converter.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.utils.MimeTypeUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FormatService {

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> documents = new LinkedHashMap<>();
        documents.put(List.of(Format.DOC), List.of(Format.DOCX, Format.TXT, Format.PDF, Format.EPUB, Format.RTF, Format.TIFF));
        documents.put(List.of(Format.DOCX), List.of(Format.DOC, Format.TXT, Format.PDF, Format.EPUB, Format.RTF, Format.TIFF));
        documents.put(List.of(Format.PDF), List.of(Format.DOC, Format.DOCX, Format.EPUB, Format.TIFF));
        documents.put(List.of(Format.TXT, Format.TEXT), List.of(Format.PDF, Format.DOC, Format.DOCX));
        documents.put(List.of(Format.EPUB), List.of(Format.PDF, Format.DOC, Format.DOCX, Format.RTF));
        documents.put(List.of(Format.URL, Format.HTML), List.of(Format.PDF));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(Format.PNG, Format.DEVICE_PHOTO), List.of(Format.PDF, Format.JPG, Format.BMP, Format.WEBP, Format.STICKER));
        images.put(List.of(Format.JPG, Format.JPEG), List.of(Format.PDF, Format.PNG, Format.BMP, Format.WEBP, Format.STICKER));
        images.put(List.of(Format.TIFF), List.of(Format.DOC, Format.DOCX, Format.PDF));
        images.put(List.of(Format.BMP), List.of(Format.PDF, Format.PNG, Format.JPG, Format.WEBP, Format.STICKER));
        images.put(List.of(Format.STICKER, Format.WEBP), List.of(Format.PNG, Format.JPG, Format.PDF));
        images.put(List.of(Format.SVG), List.of(Format.PDF, Format.PNG, Format.JPG, Format.BMP, Format.WEBP, Format.STICKER));
        FORMATS.put(FormatCategory.IMAGES, images);
    }

    public Map<List<Format>, List<Format>> getFormats(FormatCategory category) {
        return FORMATS.get(category);
    }

    public List<Format> getTargetFormats(Format srcFormat) {
        for (Map.Entry<FormatCategory, Map<List<Format>, List<Format>>> categoryEntry : FORMATS.entrySet()) {
            for (Map.Entry<List<Format>, List<Format>> entry : categoryEntry.getValue().entrySet()) {
                if (entry.getKey().contains(srcFormat)) {
                    return entry.getValue();
                }
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
