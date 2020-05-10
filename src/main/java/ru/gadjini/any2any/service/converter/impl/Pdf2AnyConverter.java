package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.devices.TiffDevice;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Pdf2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private FileService fileService;

    private TelegramService telegramService;

    @Autowired
    public Pdf2AnyConverter(FormatService formatService, FileService fileService, TelegramService telegramService) {
        super(Set.of(Format.PDF), formatService);
        this.fileService = fileService;
        this.telegramService = telegramService;
    }

    @Override
    public ConvertResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.TIFF) {
            return toTiff(fileQueueItem);
        }

        return doConvert(fileQueueItem);
    }

    private FileResult toTiff(FileQueueItem queueItem) {
        File pdfFile = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(pdfFile.getAbsolutePath());
            try {
                TiffDevice tiffDevice = new TiffDevice();
                File tiff = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), "tiff"));
                tiffDevice.process(pdf, tiff.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(tiff, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                pdf.dispose();
            }
        } finally {
            FileUtils.deleteQuietly(pdfFile);
        }
    }

    private FileResult doConvert(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document(file.getAbsolutePath());
            try {
                File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
                document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case DOC:
                return SaveFormat.Doc;
            case DOCX:
                return SaveFormat.DocX;
            case EPUB:
                return SaveFormat.Epub;
        }

        throw new UnsupportedOperationException();
    }
}
