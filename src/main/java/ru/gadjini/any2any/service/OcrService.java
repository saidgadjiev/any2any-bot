package ru.gadjini.any2any.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TextExtractionFailedException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.job.CommonJobExecutor;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;

import java.util.List;
import java.util.Locale;

@Service
public class OcrService {

    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            new Locale("ru"),
            new Locale("en")
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    private static final String TESSDATA_PATH = "tessdata";

    private TelegramService telegramService;

    private CommonJobExecutor commonJobExecutor;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public OcrService(TelegramService telegramService, CommonJobExecutor commonJobExecutor, @Qualifier("limits") MessageService messageService,
                      UserService userService, LocalisationService localisationService) {
        this.telegramService = telegramService;
        this.commonJobExecutor = commonJobExecutor;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    public void extractText(int userId, Any2AnyFile any2AnyFile, Locale ocrLocale) {
        commonJobExecutor.addJob(() -> {
            LOGGER.debug("Start ocr. File id " + any2AnyFile.getFileId());
            SmartTempFile file = telegramService.downloadFileByFileId(any2AnyFile.getFileId(), any2AnyFile.getFormat().getExt());
            ITesseract tesseract = new Tesseract();
            tesseract.setLanguage(ocrLocale.getISO3Language());
            tesseract.setDatapath(TESSDATA_PATH);

            Locale locale = userService.getLocaleOrDefault(userId);
            try {
                String result = tesseract.doOCR(file.getFile());
                result = result.replace("\n\n", "\n");
                if (StringUtils.isBlank(result)) {
                    messageService.sendMessage(new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale)));
                }
                messageService.sendMessage(new SendMessageContext(userId, result));
            } catch (Exception ex) {
                messageService.sendErrorMessage(userId, locale);
                throw new TextExtractionFailedException(ex);
            } finally {
                file.smartDelete();
            }
        });
    }
}
