package ru.gadjini.any2any.service.conversion;

import com.aspose.pdf.Document;
import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.event.QueueItemCanceled;
import ru.gadjini.any2any.exception.CorruptedFileException;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.object.User;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Any2AnyConverter;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueBusinessService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueService;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ConvertionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertionService.class);

    private final Map<Integer, Future<?>> processing = new ConcurrentHashMap<>();

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionQueueBusinessService queueBusinessService;

    private ConversionQueueService queueService;

    private LocalisationService localisationService;

    private UserService userService;

    private ThreadPoolExecutor executor;

    @Autowired
    public ConvertionService(@Qualifier("limits") MessageService messageService,
                             ConversionQueueBusinessService conversionQueueService,
                             LocalisationService localisationService, UserService userService,
                             Set<Any2AnyConverter> any2AnyConvertersSet, InlineKeyboardService inlineKeyboardService,
                             ConversionQueueService queueService) {
        this.messageService = messageService;
        this.queueBusinessService = conversionQueueService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.queueService = queueService;
        any2AnyConvertersSet.forEach(any2AnyConverters::add);
    }

    @PostConstruct
    public void init() {
        initFonts();
        applyAsposeLicenses();
        queueBusinessService.resetProcessing();
    }

    @EventListener
    public void queueItemCanceled(QueueItemCanceled queueItemCanceled) {
        Future<?> future = processing.get(queueItemCanceled.getId());
        if (future != null && (!future.isCancelled() || !future.isDone())) {
            future.cancel(true);
        }
    }

    @Autowired
    public void setExecutor(@Qualifier("conversionTaskExecutor") ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public void rejectTask(ConversionTask conversionTask) {
        queueBusinessService.setWaiting(conversionTask.fileQueueItem.getId());
    }

    public ConversionTask getTask() {
        synchronized (this) {
            ConversionQueueItem peek = queueBusinessService.takeItem();

            return new ConversionTask(peek);
        }
    }

    public ConversionQueueItem convert(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem queueItem = queueService.add(user, convertState, targetFormat);

        executor.execute(new ConversionTask(queueItem));

        return queueItem;
    }

    private void initFonts() {
        LOGGER.debug("Pdf fonts paths " + Document.getLocalFontPaths());
    }

    private void applyAsposeLicenses() {
        try {
            new License().setLicense("license/license-19.lic");
            LOGGER.debug("Word license applied");

            new com.aspose.pdf.License().setLicense("license/license-19.lic");
            LOGGER.debug("Pdf license applied");

            new com.aspose.imaging.License().setLicense("license/license-19.lic");
            LOGGER.debug("Imaging license applied");

            new com.aspose.slides.License().setLicense("license/license-19.lic");
            LOGGER.debug("Slides license applied");

            new com.aspose.cells.License().setLicense("license/license-19.lic");
            LOGGER.debug("Cells license applied");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public class ConversionTask implements Runnable {

        private final ConversionQueueItem fileQueueItem;

        private ConversionTask(ConversionQueueItem fileQueueItem) {
            this.fileQueueItem = fileQueueItem;
        }

        @Override
        public void run() {
            try {
                convert(fileQueueItem);
            } finally {
                processing.remove(fileQueueItem.getId());
            }
        }

        private void convert(ConversionQueueItem fileQueueItem) {
            Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
            if (candidate != null) {
                LOGGER.debug("Start conversion for user " + fileQueueItem.getUserId() + " from " + fileQueueItem.getFormat() + " to " + fileQueueItem.getTargetFormat() + " id " + fileQueueItem.getId());
                try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                    sendResult(fileQueueItem, convertResult);
                    queueBusinessService.complete(fileQueueItem.getId());
                    LOGGER.debug(
                            "Convert from {} to {} has taken {}. File size {} id {}",
                            fileQueueItem.getFormat(),
                            fileQueueItem.getTargetFormat(),
                            convertResult.time(),
                            fileQueueItem.getSize(),
                            fileQueueItem.getId()
                    );
                } catch (CorruptedFileException ex) {
                    queueBusinessService.completeWithException(fileQueueItem.getId(), ex.getMessage());
                    LOGGER.error(ex.getMessage());
                    Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                    messageService.sendMessage(
                            new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_DAMAGED_FILE, locale))
                                    .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                    );
                } catch (Exception ex) {
                    Future<?> future = processing.get(fileQueueItem.getId());
                    if (future != null && future.isCancelled()) {
                        LOGGER.debug("Conversion " + fileQueueItem.getId() + " canceled. From " + fileQueueItem.getFormat() + " to " + fileQueueItem.getTargetFormat() + " fileId " + fileQueueItem.getFileId());
                    } else {
                        queueBusinessService.exception(fileQueueItem.getId(), ex);
                        LOGGER.error(ex.getMessage(), ex);
                        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                        messageService.sendMessage(
                                new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERSION_FAILED, locale))
                                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                        );
                    }
                }
            } else {
                queueBusinessService.converterNotFound(fileQueueItem.getId());
                LOGGER.debug("Candidate not found for: " + fileQueueItem.getFormat());
            }
        }

        private Any2AnyConverter<ConvertResult> getCandidate(ConversionQueueItem fileQueueItem) {
            for (Any2AnyConverter<ConvertResult> any2AnyConverter : any2AnyConverters) {
                if (any2AnyConverter.accept(fileQueueItem.getFormat(), fileQueueItem.getTargetFormat())) {
                    return any2AnyConverter;
                }
            }

            return null;
        }

        private void sendResult(ConversionQueueItem fileQueueItem, ConvertResult convertResult) {
            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            switch (convertResult.resultType()) {
                case FILE: {
                    SendDocument sendDocumentContext = new SendDocument((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                            .setCaption(fileQueueItem.getMessage())
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            .setReplyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    try {
                        messageService.sendDocument(sendDocumentContext);
                    } catch (TelegramRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendDocumentContext.setReplyToMessageId(null);
                            messageService.sendDocument(sendDocumentContext);
                        } else {
                            throw ex;
                        }
                    }
                    break;
                }
                case STICKER: {
                    SendSticker sendFileContext = new SendSticker((long) fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            .setReplyMarkup(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                    try {
                        messageService.sendSticker(sendFileContext);
                    } catch (TelegramRequestException ex) {
                        if (ex.getErrorCode() == 400 && ex.getMessage().contains("reply message not found")) {
                            LOGGER.debug("Reply message not found try send without reply");
                            sendFileContext.setReplyToMessageId(null);
                            messageService.sendSticker(sendFileContext);
                        } else {
                            throw ex;
                        }
                    }
                    break;
                }
            }
        }
    }
}
