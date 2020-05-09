package ru.gadjini.any2any.service.converter.impl;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatService;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

@Component
public class PowerPoint2AnyConverter extends BaseAny2AnyConverter<FileResult> {

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

    private FileService fileService;

    @Autowired
    public PowerPoint2AnyConverter(TelegramService telegramService, FileService fileService, FormatService formatService) {
        super(ACCEPT_FORMATS, formatService);
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem, Format targetFormat) {
        switch (targetFormat) {
            case PDF:
                return toPdf(fileQueueItem);
        }
        throw new IllegalArgumentException();
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File file = telegramService.downloadFileByFileId(fileQueueItem.getFileId());

        try (InputStream inputStream = new FileInputStream(file)) {
            Presentation presentation = new Presentation(inputStream);
            File tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);
            presentation.dispose();
            return new FileResult(tempFile);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    public static void main(String[] args) {
        try {
            Presentation presentation = new Presentation("example.pptx");
            File tempFile = Files.createFile(Paths.get("example.pdf")).toFile();
            presentation.save(tempFile.getAbsolutePath(), SaveFormat.Pdf);
            System.out.println(tempFile.toString());
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
