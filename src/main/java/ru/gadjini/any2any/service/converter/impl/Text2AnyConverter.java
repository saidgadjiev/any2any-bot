package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Set;

@Component
public class Text2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Position PDF_PAGE_START_POSITION = new Position(10, 762);

    private FileService fileService;

    @Autowired
    public Text2AnyConverter(FormatService formatService, FileService fileService) {
        super(Set.of(Format.TEXT), formatService);
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem, Format targetFormat) {
        if (targetFormat == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        throw new UnsupportedOperationException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        try {
            Document document = new Document();
            Page page = document.getPages().add();
            TextFragment textFragment = new TextFragment(fileQueueItem.getFileId());
            textFragment.getTextState().setFont(FontRepository.findFont("Verdana"));
            textFragment.getTextState().setFontSize(14);
            textFragment.setPosition(PDF_PAGE_START_POSITION);
            TextBuilder textBuilder = new TextBuilder(page);
            textBuilder.appendText(textFragment);

            File file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            document.save(file.getAbsolutePath());

            return new FileResult(file);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
