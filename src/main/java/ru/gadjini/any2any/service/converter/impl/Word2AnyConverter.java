package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.devices.TiffDevice;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
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
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Word2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.DOC, Format.DOCX);

    private TelegramService telegramService;

    private TempFileService fileService;

    @Autowired
    public Word2AnyConverter(TelegramService telegramService, TempFileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem queueItem) {
        if (queueItem.getTargetFormat() == Format.TIFF) {
            return toTiff(queueItem);
        }
        
        return doConvert(queueItem);
    }

    private FileResult toTiff(FileQueueItem queueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document word = new Document(file.getAbsolutePath());
            SmartTempFile pdfFile = fileService.createTempFile0("any2any", "pdf");
            try {
                word.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
            } finally {
                word.cleanup();
            }
            com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(pdfFile.getAbsolutePath());
            SmartTempFile tiff = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), "tiff"));
            try {
                TiffDevice tiffDevice = new TiffDevice();
                tiffDevice.process(pdf, tiff.getAbsolutePath());
            } finally {
                pdf.dispose();
                pdfFile.smartDelete();
            }

            stopWatch.stop();
            return new FileResult(tiff, stopWatch.getTime());
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvert(FileQueueItem queueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document asposeDocument = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), queueItem.getTargetFormat().getExt()));
                asposeDocument.save(result.getAbsolutePath(), getSaveFormat(queueItem.getTargetFormat()));

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                asposeDocument.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.PDF;
            case EPUB:
                return SaveFormat.EPUB;
            case RTF:
                return SaveFormat.RTF;
            case DOCX:
                return SaveFormat.DOCX;
            case DOC:
                return SaveFormat.DOC;
            case TXT:
                return SaveFormat.TEXT;
        }

        throw new UnsupportedOperationException();
    }
}
