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
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.file.FileManager;
import ru.gadjini.any2any.utils.MimeTypeUtils;

import java.io.File;

import static ru.gadjini.any2any.service.conversion.api.Format.*;

@Service
public class FormatService {

    private static final String TAG = "format";

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatService.class);

    private FileManager fileManager;

    private TempFileService tempFileService;

    @Autowired
    public FormatService(FileManager fileManager, TempFileService tempFileService) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
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

    public String getExt(String fileName, String mimeType) {
        String extension = MimeTypeUtils.getExtension(mimeType);

        if (StringUtils.isNotBlank(extension) && !".bin".equals(extension)) {
            extension = extension.substring(1);
            if (extension.equals("mpga")) {
                return "mp3";
            }
        } else {
            extension = FilenameUtils.getExtension(fileName);
        }

        if ("jpeg".equals(extension)) {
            return "jpg";
        }

        return StringUtils.isBlank(extension) ? "bin" : extension;
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

    public Format getImageFormat(long chatId, String photoFileId, long fileSize) {
        SmartTempFile file = tempFileService.createTempFile(chatId, photoFileId, TAG, "tmp");

        try {
            fileManager.downloadFileByFileId(photoFileId, fileSize, file);
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
