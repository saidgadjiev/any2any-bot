package ru.gadjini.any2any.service.converter.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.converter.api.result.StickerResult;
import ru.gadjini.any2any.service.image.device.ImageConvertDevice;
import ru.gadjini.any2any.service.image.trace.ImageTracer;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PHOTO, Format.HEIC, Format.HEIF, Format.PNG, Format.SVG,
           Format.JP2, Format.JPG, Format.BMP, Format.WEBP);

    private TelegramService telegramService;

    private TempFileService fileService;

    private ImageConvertDevice imageDevice;

    private ImageTracer imageTracer;

    @Autowired
    public Image2AnyConverter(TelegramService telegramService, TempFileService fileService,
                              FormatService formatService, ImageConvertDevice imageDevice, ImageTracer imageTracer) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
        this.imageTracer = imageTracer;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.HEIC && fileQueueItem.getTargetFormat() == Format.PDF) {
            return doConvertHeicToPdf(fileQueueItem);
        }
        if (fileQueueItem.getTargetFormat() == Format.ICO) {
            return doConvertToIco(fileQueueItem);
        }
        if (fileQueueItem.getTargetFormat() == Format.SVG) {
            return doConvertToSvg(fileQueueItem);
        }

        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat() != Format.PHOTO ? fileQueueItem.getFormat().getExt() : "tmp");
        normalize(file.getFile(), fileQueueItem);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));

            imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());

            stopWatch.stop();
            return fileQueueItem.getTargetFormat() == Format.STICKER
                    ? new StickerResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS))
                    : new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvertHeicToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "png"));
            try {
                imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
                imageDevice.convert(tempFile.getAbsolutePath(), result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                tempFile.smartDelete();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private void normalize(File file, ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.PHOTO) {
            Format format = formatService.getImageFormat(file, fileQueueItem.getFileId());
            format = format == null ? Format.JPG : format;
            fileQueueItem.setFormat(format);
        }
    }

    private FileResult doConvertToIco(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));

            imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath(),
                    "-resize", "x32", "-gravity", "center", "-crop", "32x32+0+0", "-flatten", "-colors", "256");

            stopWatch.stop();
            return new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvertToSvg(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "svg"));
            if (fileQueueItem.getTargetFormat() != Format.PNG) {
                SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "png"));
                try {
                    imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath());
                    imageTracer.trace(tempFile.getAbsolutePath(), result.getAbsolutePath());

                    stopWatch.stop();
                    return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    tempFile.smartDelete();
                }
            } else {
                imageTracer.trace(file.getAbsolutePath(), result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}
