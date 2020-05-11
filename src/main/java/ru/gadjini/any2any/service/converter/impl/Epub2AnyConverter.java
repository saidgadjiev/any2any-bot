package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.EpubLoadOptions;
import com.aspose.pdf.SaveFormat;
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
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Epub2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Epub2AnyConverter(FormatService formatService, TelegramService telegramService, FileService fileService) {
        super(Set.of(Format.EPUB), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public ConvertResult convert(FileQueueItem fileQueueItem) {
        switch (fileQueueItem.getTargetFormat()) {
            case RTF:
                return toRtf(fileQueueItem);
            default:
                return doConvert(fileQueueItem);
        }
    }

    private FileResult toRtf(FileQueueItem fileQueueItem) {
        File epub = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            File doc = toDoc(epub);
            try {
                File rtf = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
                toRtf(doc, rtf);

                stopWatch.stop();
                return new FileResult(rtf, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                FileUtils.deleteQuietly(doc);
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(epub);
        }
    }

    private FileResult doConvert(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document(file.getAbsolutePath(), new EpubLoadOptions());
            try {
                File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
                document.save(result.getAbsolutePath(), getSaveFormat(fileQueueItem.getTargetFormat()));

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    private void toRtf(File doc, File outFile) throws Exception {
        com.aspose.words.Document document = new com.aspose.words.Document(doc.getAbsolutePath());
        try {
            document.save(outFile.getAbsolutePath(), com.aspose.words.SaveFormat.RTF);
        } finally {
            document.cleanup();
        }
    }

    private File toDoc(File epub) {
        Document document = new Document(epub.getAbsolutePath(), new EpubLoadOptions());
        try {
            File result = fileService.createTempFile("any2any", "doc");
            document.save(result.getAbsolutePath(), SaveFormat.Doc);

            return result;
        } finally {
            document.dispose();
        }
    }

    private int getSaveFormat(Format format) {
        switch (format) {
            case PDF:
                return SaveFormat.Pdf;
            case DOC:
                return SaveFormat.Doc;
            case DOCX:
                return SaveFormat.DocX;
            case RTF:
        }

        throw new UnsupportedOperationException();
    }
}
