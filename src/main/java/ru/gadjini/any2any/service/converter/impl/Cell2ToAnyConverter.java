package ru.gadjini.any2any.service.converter.impl;

import com.aspose.cells.Workbook;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Worksheet;
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
public class Cell2ToAnyConverter extends BaseAny2AnyConverter<FileResult> {

    private static final Set<Format> ACCEPT_FORMATS = Set.of(Format.XLS, Format.XLSX);

    private TelegramService telegramService;

    private FileService fileService;

    @Autowired
    public Cell2ToAnyConverter(TelegramService telegramService, FileService fileService, FormatService formatService) {
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
            Workbook workbook = new Workbook(inputStream);
            File tempFile = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
            workbook.save(tempFile.getAbsolutePath(), SaveFormat.PDF);
            workbook.dispose();
            return new FileResult(tempFile);
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    public static void main(String[] args) {
        try {
            Workbook workbook = new Workbook("dd.numbers");
            File tempFile = Files.createFile(Paths.get("example.pdf")).toFile();
//            File tempFile = Files.createFile(Paths.get("example.pdf")).toFile();
//            workbook.save(tempFile.getAbsolutePath(), SaveFormat.PDF);
//            System.out.println(tempFile.toString());
//
            // Get the count of the worksheets in the workbook
            int sheetCount = workbook.getWorksheets().getCount();

// Make all sheets invisible except first worksheet
            for (int i = 1; i < workbook.getWorksheets().getCount(); i++) {
                workbook.getWorksheets().get(i).setVisible(false);
            }

// Take Pdfs of each sheet
            for (int j = 0; j < workbook.getWorksheets().getCount(); j++) {
                Worksheet ws = workbook.getWorksheets().get(j);
                workbook.save("/Users/magomed/IdeaProjects/any2any-bot/" + ws.getName() + ".pdf");

                if (j < workbook.getWorksheets().getCount() - 1) {
                    workbook.getWorksheets().get(j + 1).setVisible(true);
                    workbook.getWorksheets().get(j).setVisible(false);
                }
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }
}
