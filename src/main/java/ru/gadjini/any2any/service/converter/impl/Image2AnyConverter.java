package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.ImageOptionsBase;
import com.aspose.imaging.LoadOptions;
import com.aspose.imaging.fileformats.pdf.PdfDocumentInfo;
import com.aspose.imaging.fileformats.png.PngColorType;
import com.aspose.imaging.imageloadoptions.PngLoadOptions;
import com.aspose.imaging.imageloadoptions.SvgLoadOptions;
import com.aspose.imaging.imageoptions.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.converter.api.result.StickerResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PNG, Format.STICKER, Format.SVG, Format.JPEG, Format.JPG, Format.DEVICE_PHOTO);

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Image2AnyConverter(TelegramService telegramService, FileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (Image image = Image.load(file.getAbsolutePath(), getLoadOptions(fileQueueItem.getFormat()))) {
            File tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
            image.save(tempFile.getAbsolutePath(), getSaveOptions(image, fileQueueItem.getFormat(), fileQueueItem.getTargetFormat()));

            stopWatch.stop();
            return fileQueueItem.getTargetFormat() == Format.STICKER
                    ? new StickerResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS))
                    : new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    private LoadOptions getLoadOptions(Format format) {
        switch (format) {
            case PNG:
            case DEVICE_PHOTO:
                return new PngLoadOptions();
            case SVG:
                return new SvgLoadOptions();
            default:
                return null;
        }
    }

    private ImageOptionsBase getSaveOptions(Image image, Format format, Format targetFormat) {
        switch (targetFormat) {
            case PDF:
                if (format == Format.SVG) {
                    PdfOptions pdfOptions = new PdfOptions();
                    SvgRasterizationOptions rasterizationOptions = new SvgRasterizationOptions();
                    rasterizationOptions.setPageHeight(image.getHeight());
                    rasterizationOptions.setPageWidth(image.getWidth());
                    pdfOptions.setVectorRasterizationOptions(rasterizationOptions);
                    pdfOptions.setPdfDocumentInfo(new PdfDocumentInfo());
                    return pdfOptions;
                }
                PdfOptions pdfOptions = new PdfOptions();
                pdfOptions.setPdfDocumentInfo(new PdfDocumentInfo());
                return pdfOptions;
            case PNG:
                PngOptions pngOptions = new PngOptions();
                pngOptions.setColorType(PngColorType.TruecolorWithAlpha);
                return pngOptions;
            case BMP:
                return new BmpOptions();
            case JPG:
                JpegOptions jpegOptions = new JpegOptions();
                jpegOptions.setQuality(100);
                return jpegOptions;
            case STICKER:
            case WEBP:
                WebPOptions webPOptions = new WebPOptions();
                webPOptions.setQuality(100);
                return webPOptions;
        }

        return null;
    }
}
