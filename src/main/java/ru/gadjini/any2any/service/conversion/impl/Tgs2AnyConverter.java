package ru.gadjini.any2any.service.conversion.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.ProcessExecutor;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.file.FileManager;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Tgs2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    public static final String TAG = "tgs2";

    private FileManager fileManager;

    private TempFileService fileService;

    private ArchiveService archiveService;

    @Autowired
    public Tgs2AnyConverter(FormatService formatService, FileManager fileManager,
                            TempFileService fileService) {
        super(Set.of(Format.TGS), formatService);
        this.fileManager = fileManager;
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
        SmartTempFile file = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, fileQueueItem.getFormat().getExt());

        try {
            fileManager.downloadFileByFileId(fileQueueItem.getUserId(), fileQueueItem.getFileId(), file);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFileId(), TAG, Format.GIF.getExt());
            try {
                new ProcessExecutor().execute(command(file.getAbsolutePath(), result.getAbsolutePath()));
                SmartTempFile archive = archiveService.createArchive(fileQueueItem.getUserId(), List.of(result.getFile()), Format.ZIP);

                stopWatch.stop();
                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), Format.GIF.getExt());
                return new FileResult(fileName, archive, stopWatch.getTime(TimeUnit.SECONDS));
            } catch (Exception ex) {
                result.smartDelete();
                throw ex;
            }
        } finally {
            file.smartDelete();
        }
    }

    private String[] command(String in, String out) {
        return new String[]{"node", "tgs-to-gif/cli.js", in, "--out", out};
    }
}
