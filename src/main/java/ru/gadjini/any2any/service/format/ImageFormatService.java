package ru.gadjini.any2any.service.format;

import com.aspose.imaging.FileFormat;
import com.aspose.imaging.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;

import java.io.File;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class ImageFormatService {

    private static final String TAG = "imageformat";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFormatService.class);

    private FileManager fileManager;

    private TempFileService tempFileService;

    @Autowired
    public ImageFormatService(FileManager fileManager, TempFileService tempFileService) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
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
