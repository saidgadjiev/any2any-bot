package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Text2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private FileService fileService;

    @Autowired
    public Text2AnyConverter(FormatService formatService, FileService fileService) {
        super(Set.of(Format.TEXT), formatService);
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        throw new UnsupportedOperationException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document();
            try {
                Page page = document.getPages().add();
                TextFragment textFragment = new TextFragment(fileQueueItem.getFileId());
                textFragment.getTextState().setFont(FontRepository.findFont("Verdana"));
                textFragment.getTextState().setFontSize(12);
                page.getParagraphs().add(textFragment);

                File file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
                document.save(file.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(file, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
