package ru.gadjini.any2any.service.converter.impl;

import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.result.FileResult;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Set;

@Component
public class Word2AnyConverter implements Any2AnyConverter<FileResult> {

    private static final Set<String> ACCEPT_EXTENSIONS = Set.of("doc", "docx");

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Word2AnyConverter(TelegramService telegramService, FileService fileService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    @PostConstruct
    public void init() {
        applyLicense();
    }

    @Override
    public FileResult convert(FileQueueItem queueItem) {
        File file = telegramService.downloadFileByFileId(queueItem.getFileId());

        try {
            com.aspose.words.Document asposeDocument = new com.aspose.words.Document(file.getAbsolutePath());
            File pdfFile = fileService.createTempFile(getPdfFileName(queueItem.getFileName()));
            asposeDocument.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);

            return new FileResult(pdfFile);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @Override
    public boolean accept(String extension) {
        return ACCEPT_EXTENSIONS.contains(extension);
    }

    private String getPdfFileName(String fileName) {
        return FilenameUtils.removeExtension(fileName) + ".pdf";
    }

    private void applyLicense() {
        try {
            new License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
