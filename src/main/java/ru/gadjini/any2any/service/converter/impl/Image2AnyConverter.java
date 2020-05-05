package ru.gadjini.any2any.service.converter.impl;

import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.pdf.PdfDocumentInfo;
import com.aspose.imaging.fileformats.png.PngImage;
import com.aspose.imaging.imageoptions.PdfOptions;
import com.aspose.imaging.internal.ls.P;
import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.FileResult;

import java.io.*;
import java.util.Set;

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
    public FileResult convert(FileQueueItem fileQueueItem, Format targetFormat) {
        switch (targetFormat) {
            case PDF:
                return toPdf(fileQueueItem);
        }

        throw new IllegalArgumentException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId());

        try (InputStream inputStream = new FileInputStream(file); Image image = Image.load(inputStream)) {
            PdfOptions pdfOptions = new PdfOptions();
            pdfOptions.setPdfDocumentInfo(new PdfDocumentInfo());

            File tempFile = fileService.createTempFile(fileQueueItem.getFileName() + ".pdf");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                image.save(outputStream, pdfOptions);
            }

            return new FileResult(tempFile);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
