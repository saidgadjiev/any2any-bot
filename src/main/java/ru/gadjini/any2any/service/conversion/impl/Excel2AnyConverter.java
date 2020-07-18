package ru.gadjini.any2any.service.conversion.impl;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Excel2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final String TAG = "excel2";

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.XLS, Format.XLSX);

    private TelegramService telegramService;

    private TempFileService fileService;

    @Autowired
    public Excel2AnyConverter(TelegramService telegramService, TempFileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(ConversionQueueItem fileQueueItem) {
        return toPdf(fileQueueItem);
    }

    private FileResult toPdf(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileService.createTempFile0(TAG, fileQueueItem.getFormat().getExt());
        telegramService.downloadFileByFileId(fileQueueItem.getFileId(), file);

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Workbook workbook = new Workbook(file.getAbsolutePath());
            try {
                SmartTempFile tempFile = fileService.createTempFile0(TAG, Format.PDF.getExt());
                workbook.save(tempFile.getAbsolutePath(), SaveFormat.PDF);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.PDF.getExt());
                return new FileResult(fileName, tempFile, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                workbook.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            file.smartDelete();
        }
    }
}
