package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.pdf.PdfDocumentInfo;
import com.aspose.imaging.imageoptions.PdfOptions;
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
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Image2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PNG, Format.JPEG, Format.JPG, Format.DEVICE_PHOTO);

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
        switch (fileQueueItem.getTargetFormat()) {
            case PDF:
                return toPdf(fileQueueItem);
        }

        throw new IllegalArgumentException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (Image image = Image.load(file.getAbsolutePath())) {
            PdfOptions pdfOptions = new PdfOptions();
            pdfOptions.setPdfDocumentInfo(new PdfDocumentInfo());

            File tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            image.save(tempFile.getAbsolutePath(), pdfOptions);

            stopWatch.stop();
            return new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
