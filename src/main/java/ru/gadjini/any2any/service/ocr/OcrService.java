package ru.gadjini.any2any.service.ocr;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloader;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Service
public class OcrService {

    public static final String TAG = "ocr";

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    private FileDownloader fileDownloader;

    private ThreadPoolTaskExecutor executor;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    private TempFileService fileService;

    private OcrDevice ocrDevice;

    @Autowired
    public OcrService(FileDownloader fileDownloader, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor,
                      @Qualifier("messageLimits") MessageService messageService,
                      UserService userService, LocalisationService localisationService, TempFileService fileService, OcrDevice ocrDevice) {
        this.fileDownloader = fileDownloader;
        this.executor = executor;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.ocrDevice = ocrDevice;
    }

    public void extractText(int userId, MessageMedia any2AnyFile) {
        executor.execute(() -> {
            LOGGER.debug("Start({}, {})", userId, any2AnyFile.getFileId());

            Locale locale = userService.getLocaleOrDefault(userId);
            SmartTempFile file = fileService.createTempFile(userId, any2AnyFile.getFileId(), TAG, any2AnyFile.getFormat().getExt());
            try {
                fileDownloader.downloadFileByFileId(any2AnyFile.getFileId(), any2AnyFile.getFileSize(), file, false);

                String result = ocrDevice.getText(file.getAbsolutePath());
                if (StringUtils.isBlank(result)) {
                    messageService.sendMessage(SendMessage.builder()
                            .chatId(String.valueOf(userId)).text(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale))
                            .parseMode(ParseMode.HTML).build());
                } else {
                    messageService.sendMessage(new SendMessage(String.valueOf(userId), result));
                }
                LOGGER.debug("Finish({}, {})", userId, StringUtils.substring(result, 0, 50));
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(userId))
                        .text(localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_TEXT_EXTRACTED, locale))
                        .parseMode(ParseMode.HTML).build());
            } finally {
                file.smartDelete();
            }
        });
    }
}
