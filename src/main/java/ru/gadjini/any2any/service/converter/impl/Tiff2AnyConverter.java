package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.words.DocumentBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.image.device.ImageDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tiff2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private TempFileService fileService;

    private ImageDevice imageDevice;

    @Autowired
    public Tiff2AnyConverter(FormatService formatService, TelegramService telegramService,
                             TempFileService fileService, ImageDevice imageDevice) {
        super(Set.of(Format.TIFF), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(FileQueueItem queueItem) {
        SmartTempFile tiff = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
            DocumentBuilder documentBuilder = new DocumentBuilder();
            try {
                for (TiffFrame tiffFrame : image.getFrames()) {
                    documentBuilder.insertImage(tiffFrame.toBitmap());
                }
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), queueItem.getTargetFormat().getExt()));
                documentBuilder.getDocument().save(result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                documentBuilder.getDocument().cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            tiff.smartDelete();
        }
    }

    private FileResult toPdf(FileQueueItem queueItem) {
        SmartTempFile tiff = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile pdf = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), "pdf"));
            imageDevice.convert(tiff.getAbsolutePath(), pdf.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(pdf, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            tiff.smartDelete();
        }
    }
}
