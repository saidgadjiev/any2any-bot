package ru.gadjini.any2any.service.conversion.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.words.DocumentBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tiff2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "tiff2";

    private TelegramService telegramService;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    @Autowired
    public Tiff2AnyConverter(FormatService formatService, TelegramService telegramService,
                             TempFileService fileService, ImageConvertDevice imageDevice) {
        super(Set.of(Format.TIFF), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(ConversionQueueItem queueItem) {
        SmartTempFile tiff = fileService.createTempFile(TAG, queueItem.getFormat().getExt());

        try {
            telegramService.downloadFileByFileId(queueItem.getFileId(), tiff);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
                DocumentBuilder documentBuilder = new DocumentBuilder();
                try {
                    for (TiffFrame tiffFrame : image.getFrames()) {
                        documentBuilder.insertImage(tiffFrame.toBitmap());
                    }
                    SmartTempFile result = fileService.createTempFile(TAG, queueItem.getTargetFormat().getExt());
                    documentBuilder.getDocument().save(result.getAbsolutePath());

                    stopWatch.stop();
                    String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), queueItem.getTargetFormat().getExt());
                    return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    documentBuilder.getDocument().cleanup();
                }
            } catch (Exception ex) {
                throw new ConvertException(ex);
            }
        } finally {
            tiff.smartDelete();
        }
    }

    private FileResult toPdf(ConversionQueueItem queueItem) {
        SmartTempFile tiff = fileService.createTempFile(TAG, queueItem.getFormat().getExt());
       try {
           telegramService.downloadFileByFileId(queueItem.getFileId(), tiff);

           StopWatch stopWatch = new StopWatch();
           stopWatch.start();
           SmartTempFile pdf = fileService.createTempFile(TAG, Format.PDF.getExt());
            imageDevice.convert(tiff.getAbsolutePath(), pdf.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), Format.PDF.getExt());
            return new FileResult(fileName, pdf, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            tiff.smartDelete();
        }
    }
}
