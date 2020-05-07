package ru.gadjini.any2any.service.converter.impl;

import com.aspose.words.SaveFormat;
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
public class Word2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.DOC, Format.DOCX);

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Word2AnyConverter(TelegramService telegramService, FileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem queueItem) {
        if (queueItem.getTargetFormat() == Format.PDF) {
            return toPdf(queueItem);
        }

        throw new IllegalArgumentException();
    }

    private FileResult toPdf(FileQueueItem queueItem) {
        File file = telegramService.downloadFileByFileId(queueItem.getFileId(), queueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            com.aspose.words.Document asposeDocument = new com.aspose.words.Document(file.getAbsolutePath());
            try {
                File pdfFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(queueItem.getFileName(), "pdf"));
                asposeDocument.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);

                stopWatch.stop();
                return new FileResult(pdfFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                asposeDocument.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
