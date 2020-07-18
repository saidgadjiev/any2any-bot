package ru.gadjini.any2any.service.conversion.impl;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "pp2";

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

    private TempFileService fileService;

    @Autowired
    public PowerPoint2AnyConverter(TelegramService telegramService, TempFileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile(TAG, fileQueueItem.getTargetFormat().getExt());
        telegramService.downloadFileByFileId(fileQueueItem.getFileId(), file);

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Presentation presentation = new Presentation(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = fileService.createTempFile(TAG, Format.PDF.getExt());
                presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                presentation.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}
