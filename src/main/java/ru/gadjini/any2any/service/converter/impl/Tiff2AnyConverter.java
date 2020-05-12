package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.pdf.Document;
import com.aspose.pdf.MarginInfo;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rectangle;
import com.aspose.words.DocumentBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tiff2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Tiff2AnyConverter(FormatService formatService, TelegramService telegramService, FileService fileService) {
        super(Set.of(Format.TIFF), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(FileQueueItem queueItem) {
        File tiff = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
            DocumentBuilder documentBuilder = new DocumentBuilder();
            try {
                for (TiffFrame tiffFrame : image.getFrames()) {
                    documentBuilder.insertImage(tiffFrame.toBitmap());
                }
                File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), queueItem.getTargetFormat().getExt()));
                documentBuilder.getDocument().save(result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                documentBuilder.getDocument().cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(tiff);
        }
    }

    private FileResult toPdf(FileQueueItem queueItem) {
        File tiff = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (TiffImage image = (TiffImage) Image.load(tiff.getAbsolutePath())) {
            Document document = new Document();
            try {
                for (TiffFrame tiffFrame : image.getFrames()) {
                    Page page = document.getPages().add();
                    page.getPageInfo().setMargin(new MarginInfo(0, 0, 0, 0));

                    page.setCropBox(new Rectangle(0, 0, tiffFrame.getHeight(), tiffFrame.getWidth()));
                    com.aspose.pdf.Image tiffPage = new com.aspose.pdf.Image();
                    page.getParagraphs().add(tiffPage);
                    tiffPage.setBufferedImage(tiffFrame.toBitmap());
                }
                File pdf = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), "pdf"));
                document.save(pdf.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(pdf, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } finally {
            FileUtils.deleteQuietly(tiff);
        }
    }
}
