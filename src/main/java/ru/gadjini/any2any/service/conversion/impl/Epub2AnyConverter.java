package ru.gadjini.any2any.service.conversion.impl;

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
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.conversion.device.ConvertDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Epub2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "epub2";

    private TelegramService telegramService;

    private TempFileService fileService;

    private ConvertDevice calibre;

    @Autowired
    public Epub2AnyConverter(FormatService formatService, TelegramService telegramService,
                             TempFileService fileService, @Qualifier("calibre") ConvertDevice calibre) {
        super(Set.of(Format.EPUB), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.calibre = calibre;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.DOC) {
            return toDoc(fileQueueItem);
        }
        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile0(TAG, fileQueueItem.getFormat().getExt());
        telegramService.downloadFileByFileId(fileQueueItem.getFileId(), file);
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile result = fileService.createTempFile0(TAG, fileQueueItem.getTargetFormat().getExt());
            calibre.convert(file.getAbsolutePath(), result.getAbsolutePath());

            stopWatch.stop();
            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt());
            return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            file.smartDelete();
        }
    }

    private FileResult toDoc(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile0(TAG, fileQueueItem.getFormat().getExt());
        telegramService.downloadFileByFileId(fileQueueItem.getFileId(), file);
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile resultDocx = fileService.createTempFile0(TAG, Format.DOCX.getExt());
            try {
                calibre.convert(file.getAbsolutePath(), resultDocx.getAbsolutePath());
                Document document = new Document(resultDocx.getAbsolutePath());
                try {
                    SmartTempFile result = fileService.createTempFile0(TAG, Format.DOC.getExt());
                    document.save(result.getAbsolutePath(), SaveFormat.DOC);

                    stopWatch.stop();
                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.DOC.getExt());
                    return new FileResult(fileName, result, stopWatch.getTime(TimeUnit.SECONDS));
                } finally {
                    document.cleanup();
                }
            } finally {
                resultDocx.smartDelete();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}
