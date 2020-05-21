package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.*;
import com.aspose.imaging.fileformats.pdf.PdfDocumentInfo;
import com.aspose.imaging.fileformats.png.PngColorType;
import com.aspose.imaging.imageloadoptions.PngLoadOptions;
import com.aspose.imaging.imageloadoptions.SvgLoadOptions;
import com.aspose.imaging.imageoptions.*;
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
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PNG, Format.SVG, Format.JPG, Format.BMP, Format.WEBP);

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
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (Image image = Image.load(file.getAbsolutePath(), getLoadOptions(fileQueueItem.getFormat()))) {
            SmartTempFile tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
            ImageOptionsBase saveOptions = getSaveOptions(image, fileQueueItem.getFormat(), fileQueueItem.getTargetFormat());
            if (saveOptions == null) {
                image.save(tempFile.getAbsolutePath());
            } else {
                image.save(tempFile.getAbsolutePath(), saveOptions);
            }

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

    private LoadOptions getLoadOptions(Format format) {
        switch (format) {
            case PNG:
                return new PngLoadOptions();
            case SVG:
                return new SvgLoadOptions();
            default:
                return null;
        }
    }

    private ImageOptionsBase getSaveOptions(Image image, Format format, Format targetFormat) {
        ImageOptionsBase options = null;

        switch (targetFormat) {
            case PDF:
                options = new PdfOptions();
                ((PdfOptions) options).setPdfDocumentInfo(new PdfDocumentInfo());
                break;
            case PNG:
                options = new PngOptions();
                ((PngOptions) options).setColorType(PngColorType.TruecolorWithAlpha);
                ((PngOptions) options).setCompressionLevel(0);
                break;
            case BMP:
                options = new BmpOptions();
                break;
            case JPG:
                options = new JpegOptions();
                ((JpegOptions) options).setQuality(100);
                break;
            case STICKER:
            case WEBP:
                options = new WebPOptions();
                ((WebPOptions) options).setQuality(100);
                break;
        }
        if (options != null && format == Format.SVG) {
            SvgRasterizationOptions rasterizationOptions = new SvgRasterizationOptions();
            rasterizationOptions.setPageHeight(image.getHeight());
            rasterizationOptions.setPageWidth(image.getWidth());
            rasterizationOptions.setBackgroundColor(Color.getTransparent());
            rasterizationOptions.setSmoothingMode(SmoothingMode.HighQuality);
            rasterizationOptions.setResolutionSettings(new ResolutionSetting(200f, 200f));
            options.setVectorRasterizationOptions(rasterizationOptions);
        }

        return options;
    }
}
