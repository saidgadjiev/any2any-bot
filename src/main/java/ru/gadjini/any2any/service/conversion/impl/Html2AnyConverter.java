package ru.gadjini.any2any.service.conversion.impl;

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
import ru.gadjini.any2any.service.html.HtmlDevice;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Html2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private TempFileService fileService;

    private HtmlDevice htmlDevice;

    @Autowired
    public Html2AnyConverter(FormatService formatService, TelegramService telegramService,
                             TempFileService fileService, @Qualifier("api") HtmlDevice htmlDevice) {
        super(Set.of(Format.URL, Format.HTML), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.htmlDevice = htmlDevice;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getFormat() == Format.URL) {
            return urlToPdf(fileQueueItem);
        }

        return htmlToPdf(fileQueueItem);
    }

    private FileResult htmlToPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile html = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            htmlDevice.processHtml(html.getAbsolutePath(), file.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(file, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            html.smartDelete();
        }
    }

    private FileResult urlToPdf(ConversionQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            SmartTempFile file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            htmlDevice.processUrl(fileQueueItem.getFileId(), file.getAbsolutePath());

            stopWatch.stop();
            return new FileResult(file, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
