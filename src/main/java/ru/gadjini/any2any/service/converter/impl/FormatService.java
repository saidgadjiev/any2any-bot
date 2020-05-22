package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.FileFormat;
import com.aspose.imaging.Image;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
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
        documents.put(List.of(DOC), List.of(PDF, DOCX, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(DOCX), List.of(PDF, DOC, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(PDF), List.of(DOC, DOCX, EPUB, TIFF));
        documents.put(List.of(TXT, TEXT), List.of(PDF, DOC, DOCX));
        documents.put(List.of(EPUB), List.of(PDF, DOC, DOCX, RTF));
        documents.put(List.of(URL, HTML), List.of(PDF));
        documents.put(List.of(XLS, XLSX), List.of(PDF));
        documents.put(List.of(PPTX, PPT, PPTM, POTX, POT, POTM, PPS, PPSX, PPSM), List.of(PDF));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(PNG), List.of(PDF, JPG, BMP, WEBP, TIFF, ICO, HEIC, STICKER));
        images.put(List.of(JPG), List.of(PDF, PNG, BMP, WEBP, TIFF, ICO, HEIC, STICKER));
        images.put(List.of(TIFF), List.of(PDF, DOCX, DOC));
        images.put(List.of(BMP), List.of(PDF, PNG, JPG, WEBP, TIFF, ICO, HEIC, STICKER));
        images.put(List.of(WEBP), List.of(PDF, PNG, JPG, BMP, TIFF, ICO, HEIC, STICKER));
        images.put(List.of(SVG), List.of(PDF, PNG, JPG, BMP, WEBP, TIFF, ICO, HEIC, STICKER));
        images.put(List.of(HEIC), List.of(PDF, PNG, JPG, BMP, WEBP, TIFF, ICO, STICKER));
        images.put(List.of(ICO), List.of(PDF, PNG, JPG, BMP, WEBP, TIFF, HEIC, STICKER));
        images.put(List.of(TGS), List.of(GIF));
        FORMATS.put(FormatCategory.IMAGES, images);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatService.class);

    private TelegramService telegramService;

    @Autowired
    public FormatService(TelegramService telegramService) {
        this.telegramService = telegramService;
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

    public Format getAssociatedFormat(String format) {
        if ("jpeg".equals(format)) {
            return JPG;
        }
        format = format.toUpperCase();
        for (Format f : values()) {
            if (f.name().equals(format)) {
                return f;
            }
        }

        return null;
    }

    public Format getFormat(String text) {
        if (isUrl(text)) {
            return URL;
        }

        return TEXT;
    }

    public String getExt(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension) && !".bin".equals(extension)) {
            extension = extension.substring(1);
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }

        if ("jpeg".equals(extension)) {
            return "jpg";
        }

        return StringUtils.isBlank(extension) ? null : extension;
    }

    public Format getFormat(String fileName, String mimeType) {
        String extension = getExt(fileName, mimeType);
        if (StringUtils.isBlank(extension)) {
            return null;
        }

        for (Format format : values()) {
            if (format.getExt().equals(extension)) {
                return format;
            }
        }

        return null;
    }

    public Format getImageFormat(String photoFileId) {
        SmartTempFile file = telegramService.downloadFileByFileId(photoFileId, "tmp");

        try {
            long format = Image.getFileFormat(file.getAbsolutePath());

            return getImageFormat(format);
        } catch (Exception ex) {
            LOGGER.error("Error format detect. FileId: " + photoFileId + ". " + ex.getMessage(), ex);
            return null;
        } finally {
            file.smartDelete();
        }
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

    private Format getImageFormat(long format) {
        if (format == FileFormat.Bmp) {
            return BMP;
        } else if (format == FileFormat.Png) {
            return PNG;
        } else if (format == FileFormat.Jpeg) {
            return JPG;
        } else if (format == FileFormat.Tiff) {
            return TIFF;
        } else if (format == FileFormat.Webp) {
            return WEBP;
        } else if (format == FileFormat.Svg) {
            return SVG;
        } else {
            return null;
        }
    }
}
