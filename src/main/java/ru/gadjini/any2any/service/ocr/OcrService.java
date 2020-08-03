package ru.gadjini.any2any.service.ocr;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.OcrException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Service
public class OcrService {

    public static final String TAG = "ocr";

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    private TelegramService telegramService;

    private ThreadPoolTaskExecutor executor;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private TempFileService fileService;

    private OcrDevice ocrDevice;

    @Autowired
    public OcrService(TelegramService telegramService, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                      @Qualifier("limits") MessageService messageService,
                      UserService userService, LocalisationService localisationService, TempFileService fileService, OcrDevice ocrDevice) {
        this.telegramService = telegramService;
        this.executor = executor;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.ocrDevice = ocrDevice;
    }

    public void extractText(int userId, Any2AnyFile any2AnyFile) {
        executor.execute(() -> {
            LOGGER.debug("Start({}, {})", userId, any2AnyFile.getFileId());

            Locale locale = userService.getLocaleOrDefault(userId);
            SmartTempFile file = fileService.createTempFile(userId, any2AnyFile.getFileId(), TAG, any2AnyFile.getFormat().getExt());
            try {
                telegramService.downloadFileByFileId(any2AnyFile.getFileId(), file);

                String result = ocrDevice.getText(file.getAbsolutePath());
                if (StringUtils.isBlank(result)) {
                    messageService.sendMessageAsync(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale)));
                } else {
                    messageService.sendMessageAsync(new SendMessage((long) userId, result));
                }
                LOGGER.debug("Finish({}, {})", userId, StringUtils.substring(result, 0, 50));
            } catch (Exception ex) {
                messageService.sendMessageAsync(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale)));
                throw new OcrException(ex);
            } finally {
                file.smartDelete();
            }
        });
    }
}
