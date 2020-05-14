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

import static ru.gadjini.any2any.service.converter.api.Format.*;

@Service
public class FormatService {

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> documents = new LinkedHashMap<>();
        documents.put(List.of(DOC), List.of(DOCX, TXT, PDF, EPUB, RTF, TIFF));
        documents.put(List.of(DOCX), List.of(DOC, TXT, PDF, EPUB, RTF, TIFF));
        documents.put(List.of(PDF), List.of(DOC, DOCX, EPUB, TIFF));
        documents.put(List.of(TXT, TEXT), List.of(PDF, DOC, DOCX));
        documents.put(List.of(EPUB), List.of(PDF, DOC, DOCX, RTF));
        documents.put(List.of(URL, HTML), List.of(PDF));
        documents.put(List.of(XLS, XLSX), List.of(PDF));
        documents.put(List.of(PPTX, PPT, PPTM, POTX, POT, POTM, PPS, PPSX, PPSM), List.of(PDF));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(PNG, DEVICE_PHOTO), List.of(PDF, JPG, BMP, WEBP, STICKER));
        images.put(List.of(JPG, JPEG), List.of(PDF, PNG, BMP, WEBP, STICKER));
        images.put(List.of(TIFF), List.of(DOC, DOCX, PDF));
        images.put(List.of(BMP), List.of(PDF, PNG, JPG, WEBP, STICKER));
        images.put(List.of(STICKER, WEBP), List.of(PNG, JPG, PDF));
        images.put(List.of(SVG), List.of(PDF, PNG, JPG, BMP, WEBP, STICKER));
        FORMATS.put(FormatCategory.IMAGES, images);
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
            return URL;
        }

        return TEXT;
    }

    public Format getFormat(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension)) {
            extension = extension.substring(1);
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }
        if (StringUtils.isBlank(extension)) {
            return null;
        }

        extension = extension.toUpperCase();
        for (Format format: values()) {
            if (format.getExt().equals(extension)) {
                return format;
            }
        }

        return null;
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
