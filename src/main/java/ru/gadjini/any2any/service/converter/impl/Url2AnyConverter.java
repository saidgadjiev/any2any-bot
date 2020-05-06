package ru.gadjini.any2any.service.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.WkhtmltopdfService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.util.Set;

@Component
public class Url2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private FileService fileService;

    private WkhtmltopdfService wkhtmltopdfService;

    @Autowired
    protected Url2AnyConverter(FormatService formatService, FileService fileService, WkhtmltopdfService wkhtmltopdfService) {
        super(Set.of(Format.URL), formatService);
        this.fileService = fileService;
        this.wkhtmltopdfService = wkhtmltopdfService;
    }

    @Override
    public ConvertResult convert(FileQueueItem fileQueueItem, Format targetFormat) {
        if (targetFormat == Format.PDF) {
            return toPdf(fileQueueItem);
        }
        throw new UnsupportedOperationException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        try {
            File file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            wkhtmltopdfService.process(fileQueueItem.getFileId(), file.getAbsolutePath());

            return new FileResult(file);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
