package ru.gadjini.any2any.service.converter.impl;

import com.aspose.pdf.*;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.Font;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.exception.ConvertException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.text.TextDetector;
import ru.gadjini.any2any.service.text.TextDirection;
import ru.gadjini.any2any.service.text.TextInfo;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.any2any.utils.TextUtils;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class Text2AnyConverter extends BaseAny2AnyConverter<FileResult> {

    private FileService fileService;

    private TextDetector textDetector;

    @Autowired
    public Text2AnyConverter(FormatService formatService, FileService fileService, TextDetector textDetector) {
        super(Set.of(Format.TEXT), formatService);
        this.fileService = fileService;
        this.textDetector = textDetector;
    }

    @Override
    public FileResult convert(FileQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == Format.PDF) {
            return toPdf(fileQueueItem);
        }
        if (fileQueueItem.getTargetFormat() == Format.TXT) {
            return toTxt(fileQueueItem);
        }
        return toWord(fileQueueItem);
    }

    private FileResult toTxt(FileQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "txt"));
            TextInfo textInfo = textDetector.detect(fileQueueItem.getFileId());
            String text = TextUtils.removeAllEmojis(fileQueueItem.getFileId(), textInfo.getDirection());
            FileUtils.writeStringToFile(result.getFile(), text, StandardCharsets.UTF_8);

            stopWatch.stop();
            return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private FileResult toWord(FileQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            com.aspose.words.Document document = new com.aspose.words.Document();
            try {
                DocumentBuilder documentBuilder = new DocumentBuilder(document);
                Font font = documentBuilder.getFont();
                font.setColor(Color.BLACK);

                TextInfo textInfo = textDetector.detect(fileQueueItem.getFileId());
                String text = TextUtils.removeAllEmojis(fileQueueItem.getFileId(), textInfo.getDirection());
                if (textInfo.getDirection() == TextDirection.LR) {
                    font.setSize(textInfo.getFont().getPrimarySize());
                    font.setName(textInfo.getFont().getFontName());
                } else {
                    font.setSizeBi(textInfo.getFont().getPrimarySize());
                    font.setNameBi(textInfo.getFont().getFontName());
                    font.setBidi(true);
                    documentBuilder.getParagraphFormat().setBidi(true);
                }

                documentBuilder.write(text);
                SmartTempFile result = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), fileQueueItem.getTargetFormat().getExt()));
                document.save(result.getAbsolutePath());

                stopWatch.stop();
                return new FileResult(result, stopWatch.getTime(TimeUnit.SECONDS));
            } finally {
                document.cleanup();
            }
        } catch (Exception ex) {
            throw new ConvertException(ex);
        }
    }

    private FileResult toPdf(FileQueueItem fileQueueItem) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Document document = new Document();
            try {
                Page page = document.getPages().add();
                TextInfo textInfo = textDetector.detect(fileQueueItem.getFileId());
                String text = TextUtils.removeAllEmojis(fileQueueItem.getFileId(), textInfo.getDirection());

                TextFragment textFragment = new TextFragment(text.replace("\n", "\r\n"));
                textFragment.getTextState().setFont(FontRepository.findFont(textInfo.getFont().getFontName()));
                textFragment.getTextState().setFontSize(textInfo.getFont().getPrimarySize());
                if (textInfo.getDirection() == TextDirection.LR) {
                    document.setDirection(Direction.L2R);
                } else {
                    document.setDirection(Direction.R2L);
                }
                textFragment.getTextState().setLineSpacing(4.0f);
                page.getParagraphs().add(textFragment);

                SmartTempFile file = fileService.createTempFile(Any2AnyFileNameUtils.getFileName(fileQueueItem.getFileName(), "pdf"));
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
