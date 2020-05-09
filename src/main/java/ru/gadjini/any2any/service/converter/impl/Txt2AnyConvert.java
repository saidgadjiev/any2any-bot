package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
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
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class Txt2AnyConvert extends BaseAny2AnyConverter<FileResult> {

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Txt2AnyConvert(FormatService formatService, TelegramService telegramService, FileService fileService) {
        super(Set.of(Format.TXT), formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public ConvertResult convert(FileQueueItem fileQueueItem, Format targetFormat) {
        if (targetFormat == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        throw new UnsupportedOperationException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File txt = telegramService.downloadFileByFileId(fileQueueItem.getFileId());

        try {
            List<String> lines = Files.readLines(txt, StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);
            Document doc = new Document();
            Page page = doc.getPages().add();
            TextFragment text = new TextFragment(builder.toString());
            page.getParagraphs().add(text);
            File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            doc.save(result.getAbsolutePath());
            return new FileResult(result);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(txt);
        }
    }
}
