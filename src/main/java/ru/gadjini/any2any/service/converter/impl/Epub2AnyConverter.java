package ru.gadjini.any2any.service.converter.impl;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.converter.device.ConvertDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Epub2AnyConverter extends BaseAny2AnyConverter<FileResult> {

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
    public ConvertResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.DOC) {
            return toDoc(fileQueueItem);
        }
        return doConvert(fileQueueItem);
    }

    private FileResult doConvert(FileQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
            calibre.convert(file.getAbsolutePath(), result.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
        } finally {
            file.smartDelete();
        }
    }

    private FileResult toDoc(FileQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile resultDocx = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.DOCX.getExt()));
            try {
                calibre.convert(file.getAbsolutePath(), resultDocx.getAbsolutePath());
                Document document = new Document(resultDocx.getAbsolutePath());
                try {
                    SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.DOC.getExt()));
                    document.save(result.getAbsolutePath(), SaveFormat.DOC);

                    stopWatch.stop();
                    return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
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
