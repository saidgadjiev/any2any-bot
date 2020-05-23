package ru.gadjini.any2any.service.converter.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.converter.api.result.StickerResult;
import ru.gadjini.any2any.service.image.ImageDevice;
import ru.gadjini.any2any.service.image.trace.ImageTracer;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.HEIC, Format.PNG, Format.SVG, Format.JPG, Format.BMP, Format.WEBP);

    private TelegramService telegramService;

    private FileService fileService;

    private ImageDevice imageDevice;

    private ImageTracer imageTracer;

    @Autowired
    public Image2AnyConverter(TelegramService telegramService, FileService fileService,
                              FormatService formatService, ImageDevice imageDevice, ImageTracer imageTracer) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.imageDevice = imageDevice;
        this.imageTracer = imageTracer;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
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

    private FileResult doConvert(FileQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

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

    private FileResult doConvertHeicToPdf(FileQueueItem fileQueueItem) {
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

    private FileResult doConvertToIco(FileQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));

            imageDevice.convert(file.getAbsolutePath(), tempFile.getAbsolutePath(),
                    "-resize x32", "-gravity center", "-crop 32x32+0+0", "-flatten -colors 256");

            stopWatch.stop();
            return new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvertToSvg(FileQueueItem fileQueueItem) {
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
