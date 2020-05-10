package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.aspose.words.TxtLoadOptions;
import com.google.common.io.Files;
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
import ru.gadjini.any2any.util.Any2AnyFileNameUtils;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    public ConvertResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }

        return toWord(fileQueueItem);
    }

    private FileResult toWord(FileQueueItem fileQueueItem) {
        File txt = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            com.aspose.words.Document document = new com.aspose.words.Document(txt.getAbsolutePath(), new TxtLoadOptions());
            try {
                File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
                document.save(result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(txt);
        }
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        File txt = telegramService.downloadFileByFileId(fileQueueItem.getFileId(), fileQueueItem.getFormat().getExt());

        try {
            List<String> lines = Files.readLines(txt, StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Document doc = new Document();
            try {
                Page page = doc.getPages().add();
                TextFragment text = new TextFragment(builder.toString());

                page.getParagraphs().add(text);

                File result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
                doc.save(result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                doc.dispose();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        } finally {
            FileUtils.deleteQuietly(txt);
        }
    }

    public static void main(String[] args) throws Exception {
        File file = new File("C:/test.txt");

        try (PrintWriter printWriter = new PrintWriter(file.getAbsolutePath())) {
            for (int i = 0; i < 100; ++i) {
                printWriter.print("Привет я Саид");
                if (i % 10 == 0) {
                    printWriter.println();
                }
            }
        }
    }
}
