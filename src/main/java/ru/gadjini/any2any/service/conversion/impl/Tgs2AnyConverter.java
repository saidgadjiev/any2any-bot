package ru.gadjini.any2any.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tgs2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private TempFileService fileService;

    private ArchiveService archiveService;

    @Autowired
    public Tgs2AnyConverter(FormatService formatService, TelegramService telegramService,
                            TempFileService fileService) {
        super(Set.of(Format.TGS), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Autowired
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        return toGiff(fileQueueItem);
    }

    private FileResult toGiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "gif"));
            try {
                new ProcessExecutor().execute(command(file.getAbsolutePath(), result.getAbsolutePath()), 3 * 60);
                SmartTempFile archive = archiveService.createArchive(fileQueueItem.getUserId(), List.of(result.getFile()), Format.ZIP);

                stopWatch.stop();
                return new FileResult(archive, stopWatch.getTime(TimeUnit.SECONDS));
            } catch (Exception ex) {
                result.smartDelete();
                throw ex;
            }
        } finally {
            file.smartDelete();
        }
    }

    private String[] command(String in, String out) {
        return new String[] {"node", "tgs-to-gif/cli.js", in, "--out", out};
    }
}
