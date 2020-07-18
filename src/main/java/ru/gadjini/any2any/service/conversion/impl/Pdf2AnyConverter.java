package ru.gadjini.any2any.service.conversion.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.devices.TiffDevice;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.CorruptedFileException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.conversion.device.ConvertDevice;
import ru.gadjini.any2any.service.conversion.file.FileValidator;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Pdf2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "pdf2";

    private TempFileService fileService;

    private TelegramService telegramService;

    private ConvertDevice convertDevice;

    private FileValidator fileValidator;

    @Autowired
    public Pdf2AnyConverter(FormatService formatService, TempFileService fileService,
                            TelegramService telegramService, @Qualifier("calibre") ConvertDevice convertDevice,
                            FileValidator fileValidator) {
        super(Set.of(Format.PDF), formatService);
        this.fileService = fileService;
        this.telegramService = telegramService;
        this.convertDevice = convertDevice;
        this.fileValidator = fileValidator;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, fileQueueItem.getFormat().getExt());
        telegramService.downloadFileByFileId(fileQueueItem.getFileId(), file);

        try {
            boolean validPdf = fileValidator.isValidPdf(file.getFile().getAbsolutePath());
            if (!validPdf) {
                throw new CorruptedFileException("Damaged pdf file");
            }
            if (fileQueueItem.getTargetFormat() == Format.EPUB) {
                return toEpub(fileQueueItem, file);
            }
            if (fileQueueItem.getTargetFormat() == Format.TIFF) {
                return toTiff(fileQueueItem, file);
            }

            return doConvert(fileQueueItem, file);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult toTiff(ConversionQueueItem queueItem, SmartTempFile pdfFile) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document pdf = new Document(pdfFile.getAbsolutePath());
            try {
                TiffDevice tiffDevice = new TiffDevice();
                SmartTempFile tiff = fileService.createTempFile(TAG, Format.TIFF.getExt());
                tiffDevice.process(pdf, tiff.getAbsolutePath());

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), Format.TIFF.getExt());
                return new FileResult(fileName, tiff, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                pdf.dispose();
            }
        } finally {
            pdfFile.smartDelete();
        }
    }

    private FileResult toEpub(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile result = fileService.createTempFile(TAG, Format.EPUB.getExt());
            convertDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.EPUB.getExt());
            return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem, SmartTempFile file) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = fileService.createTempFile(TAG, fileQueueItem.getTargetFormat().getExt());
                document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } finally {
            file.smartDelete();
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case DOC:
                return SaveFormat.Doc;
            case DOCX:
                return SaveFormat.DocX;
        }

        throw new UnsupportedOperationException();
    }
}
