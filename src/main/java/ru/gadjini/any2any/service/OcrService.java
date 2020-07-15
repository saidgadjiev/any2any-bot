package ru.gadjini.any2any.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TextExtractionFailedException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.service.message.MessageService;

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

    private ThreadPoolTaskExecutor executor;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public OcrService(TelegramService telegramService, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                      @Qualifier("limits") MessageService messageService,
                      UserService userService, LocalisationService localisationService) {
        this.telegramService = telegramService;
        this.executor = executor;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    public void extractText(int userId, Any2AnyFile any2AnyFile, Locale ocrLocale) {
        executor.execute(() -> {
            LOGGER.debug("Start({}, {})", userId, any2AnyFile.getFileId());
            SmartTempFile file = telegramService.downloadFileByFileId(any2AnyFile.getFileId(), any2AnyFile.getFormat().getExt());
            ITesseract tesseract = new Tesseract();
            tesseract.setLanguage(ocrLocale.getISO3Language());
            tesseract.setDatapath(TESSDATA_PATH);

            Locale locale = userService.getLocaleOrDefault(userId);
            try {
                String result = tesseract.doOCR(file.getFile());
                result = result.replace("\n\n", "\n");
                if (StringUtils.isBlank(result)) {
                    messageService.sendMessage(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale)));
                }
                messageService.sendMessage(new SendMessage((long) userId, result));
                LOGGER.debug("Finish({}, {})", userId, StringUtils.substring(result, 0, 20));
            } catch (Exception ex) {
                messageService.sendErrorMessage(userId, locale);
                throw new TextExtractionFailedException(ex);
            } finally {
                file.smartDelete();
            }
        });
    }
}
