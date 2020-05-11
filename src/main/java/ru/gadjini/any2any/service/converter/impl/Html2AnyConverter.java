package ru.gadjini.any2any.service.converter.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.WkhtmltopdfService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Html2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private FileService fileService;

    private WkhtmltopdfService wkhtmltopdfService;

    @Autowired
    public Html2AnyConverter(FormatService formatService, TelegramService telegramService,
                             FileService fileService, WkhtmltopdfService wkhtmltopdfService) {
        super(Set.of(Format.URL, Format.HTML), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.wkhtmltopdfService = wkhtmltopdfService;
    }

    @Override
    public ConvertResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.URL) {
            return urlToPdf(fileQueueItem);
        }

        return htmlToPdf(fileQueueItem);
    }

    private FileResult htmlToPdf(FileQueueItem fileQueueItem) {
        File html = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            File file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            wkhtmltopdfService.process(html.getAbsolutePath(), file.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(file, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private FileResult urlToPdf(FileQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            File file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            wkhtmltopdfService.process(fileQueueItem.getFileId(), file.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(file, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
