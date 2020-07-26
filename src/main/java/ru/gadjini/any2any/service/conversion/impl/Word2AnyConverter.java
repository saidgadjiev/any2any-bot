package ru.gadjini.any2any.service.conversion.impl;

import com.aspose.pdf.devices.TiffDevice;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.conversion.device.ConvertDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Word2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "word2";

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.DOC, Format.DOCX);

    private TelegramService telegramService;

    private TempFileService fileService;

    private ConvertDevice convertDevice;

    @Autowired
    public Word2AnyConverter(TelegramService telegramService, TempFileService fileService,
                             FormatService formatService, @Qualifier("calibre") ConvertDevice convertDevice) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.convertDevice = convertDevice;
    }

    @Override
    public FileResult convert(ConversionQueueItem queueItem) {
        if (queueItem.getTargetFormat() == Format.EPUB) {
            if (queueItem.getFormat() == Format.DOCX) {
                return docxToEpub(queueItem);
            }

            return docToEpub(queueItem);
        }
        if (queueItem.getTargetFormat() == Format.TIFF) {
            return toTiff(queueItem);
        }

        return doConvert(queueItem);
    }

    private FileResult docToEpub(ConversionQueueItem queueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, queueItem.getFormat().getExt());

        try {
            telegramService.downloadFileByFileId(queueItem.getFileId(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document(file.getAbsolutePath());
            try {
                SmartTempFile in = fileService.createTempFile(TAG, Format.DOC.getExt());

                try {
                    document.save(in.getAbsolutePath(), SaveFormat.DOCX);
                    SmartTempFile result = fileService.createTempFile(TAG, Format.EPUB.getExt());
                    convertDevice.convert(in.getAbsolutePath(), result.getAbsolutePath());

                    stopWatch.stop();
                    String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), Format.EPUB.getExt());
                    return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    in.smartDelete();
                }
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult docxToEpub(ConversionQueueItem queueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, queueItem.getFormat().getExt());

        try {
            telegramService.downloadFileByFileId(queueItem.getFileId(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile result = fileService.createTempFile(TAG, Format.EPUB.getExt());
            convertDevice.convert(file.getAbsolutePath(), result.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), Format.EPUB.getExt());
            return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult toTiff(ConversionQueueItem queueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, queueItem.getFormat().getExt());

        try {
            telegramService.downloadFileByFileId(queueItem.getFileId(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document word = new Document(file.getAbsolutePath());
            SmartTempFile pdfFile = fileService.createTempFile("any2any", Format.PDF.getExt());
            try {
                word.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
            } finally {
                word.cleanup();
            }
            com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(pdfFile.getAbsolutePath());
            SmartTempFile tiff = fileService.createTempFile(TAG, Format.TIFF.getExt());
            try {
                TiffDevice tiffDevice = new TiffDevice();
                tiffDevice.process(pdf, tiff.getAbsolutePath());
            } finally {
                pdf.dispose();
                pdfFile.smartDelete();
            }

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), Format.TIFF.getExt());
            return new FileResult(fileName, tiff, stopWatch.getTime());
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }

    private FileResult doConvert(ConversionQueueItem queueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, queueItem.getFormat().getExt());

        try {
            telegramService.downloadFileByFileId(queueItem.getFileId(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document asposeDocument = new Document(file.getAbsolutePath());
            try {
                SmartTempFile result = fileService.createTempFile(TAG, queueItem.getTargetFormat().getExt());
                asposeDocument.save(result.getAbsolutePath(), getSaveFormat(queueItem.getTargetFormat()));

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), queueItem.getTargetFormat().getExt());
                return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
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
