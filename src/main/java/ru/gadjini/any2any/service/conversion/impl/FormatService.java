package ru.gadjini.any2any.service.conversion.impl;

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
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.utils.MimeTypeUtils;
import ru.gadjini.any2any.utils.UrlUtils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.any2any.service.conversion.api.Format.*;

@Service
public class FormatService {

    private static final String TAG = "format";

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> documents = new LinkedHashMap<>();
        documents.put(List.of(DOC), List.of(PDF, DOCX, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(DOCX), List.of(PDF, DOC, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(PDF), List.of(DOC, DOCX, EPUB, TIFF));
        documents.put(List.of(TEXT), List.of(PDF, DOC, DOCX, TXT));
        documents.put(List.of(TXT), List.of(PDF, DOC, DOCX));
        documents.put(List.of(EPUB), List.of(PDF, DOC, DOCX, RTF));
        documents.put(List.of(URL, HTML), List.of(PDF));
        documents.put(List.of(XLS, XLSX), List.of(PDF));
        documents.put(List.of(PPTX, PPT, PPTM, POTX, POT, POTM, PPS, PPSX, PPSM), List.of(PDF));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(PNG), List.of(PDF, DOC, DOCX, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(PHOTO), List.of(PDF, DOC, DOCX, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(JPG), List.of(PDF, DOC, DOCX, PNG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(TIFF), List.of(PDF, DOCX, DOC));
        images.put(List.of(BMP), List.of(PDF, PNG, JPG, JP2, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(WEBP), List.of(PDF, PNG, JPG, JP2, BMP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(SVG), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, STICKER));
        images.put(List.of(HEIC, HEIF), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, SVG, STICKER));
        images.put(List.of(ICO), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(JP2), List.of(PDF, PNG, JPG, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(TGS), List.of(GIF));
        FORMATS.put(FormatCategory.IMAGES, images);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatService.class);

    private TelegramService telegramService;

    private TempFileService tempFileService;

    @Autowired
    public FormatService(TelegramService telegramService, TempFileService tempFileService) {
        this.telegramService = telegramService;
        this.tempFileService = tempFileService;
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
        if (UrlUtils.isUrl(text)) {
            return URL;
        }

        return TEXT;
    }

    public String getExt(String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension) && !".bin".equals(extension)) {
            extension = extension.substring(1);
            if (extension.equals("mpga")) {
                return "mp3";
            }
        }

        return extension;
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
        SmartTempFile file = tempFileService.createTempFile(TAG, "tmp");

        try {
            telegramService.downloadFileByFileId(photoFileId, file);
            return getImageFormat(file.getFile(), photoFileId);
        } finally {
            file.smartDelete();
        }
    }

    public Format getImageFormat(File file, String photoFileId) {
        try {
            long format = Image.getFileFormat(file.getAbsolutePath());

            return getImageFormat(format);
        } catch (Exception ex) {
            LOGGER.error("Error format detect " + photoFileId + "\n" + ex.getMessage(), ex);
            return null;
        }
    }

    public boolean isConvertAvailable(Format src, Format target) {
        return getTargetFormats(src).contains(target);
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
