package ru.gadjini.any2any.service.converter.impl;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
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
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.PPTX,
            Format.PPT,
            Format.PPTM,
            Format.POTX,
            Format.POT,
            Format.POTM,
            Format.PPS,
            Format.PPSX,
            Format.PPSM);

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public PowerPoint2AnyConverter(TelegramService telegramService, FileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Presentation presentation = new Presentation(file.getAbsolutePath());
            try {
                File tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
                presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);

                stopWatch.stop();
                return new FileResult(tempFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                presentation.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}