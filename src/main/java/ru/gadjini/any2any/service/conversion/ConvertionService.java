package ru.gadjini.any2any.service.conversion;

import com.aspose.pdf.Document;
import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.CorruptedFileException;
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.object.User;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Any2AnyConverter;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.result.ConvertResult;
import ru.gadjini.any2any.service.conversion.api.result.FileResult;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class ConvertionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertionService.class);

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    private InlineKeyboardService inlineKeyboardService;

    private MessageService messageService;

    private ConversionQueueService queueService;

    private LocalisationService localisationService;

    private UserService userService;

    private SmartExecutorService executor;

    private TelegramService telegramService;

    @Autowired
    public ConvertionService(@Qualifier("limits") MessageService messageService,
                             LocalisationService localisationService, UserService userService,
                             Set<Any2AnyConverter> any2AnyConvertersSet, InlineKeyboardService inlineKeyboardService,
                             ConversionQueueService queueService, TelegramService telegramService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.queueService = queueService;
        this.telegramService = telegramService;
        any2AnyConvertersSet.forEach(any2AnyConverters::add);
    }

    @PostConstruct
    public void init() {
        initFonts();
        applyAsposeLicenses();
        queueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("conversionTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectTask(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
    }

    public ConversionTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            ConversionQueueItem peek = queueService.poll(weight);

            if (peek != null) {
                return new ConversionTask(peek);
            }
            return null;
        }
    }

    public ConversionQueueItem convert(User user, ConvertState convertState, Format targetFormat) {
        ConversionQueueItem queueItem = queueService.createProcessingItem(user, convertState, targetFormat);

        executor.execute(new ConversionTask(queueItem));

        return queueItem;
    }

    public void cancel(int jobId) {
        executor.cancelAndComplete(jobId, true);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<ConversionQueueItem> tasks = queueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (ConversionQueueItem item : tasks) {
            executor.execute(new ConversionTask(item));
        }
    }

    private void initFonts() {
        LOGGER.debug("Pdf fonts paths {}", Document.getLocalFontPaths());
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

    public class ConversionTask implements SmartExecutorService.Job {

        private final ConversionQueueItem fileQueueItem;

        private volatile Supplier<Boolean> checker;

        private volatile boolean canceledByUser;

        private ConversionTask(ConversionQueueItem fileQueueItem) {
            this.fileQueueItem = fileQueueItem;
        }

        @Override
        public void run() {
            try {
                Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
                if (candidate != null) {
                    String size = MemoryUtils.humanReadableByteCount(fileQueueItem.getSize());
                    LOGGER.debug("Start({}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId());

                    try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                        sendResult(fileQueueItem, convertResult);
                        queueService.complete(fileQueueItem.getId());
                        LOGGER.debug("Finish({}, {}, {}, {})", fileQueueItem.getUserId(), size, fileQueueItem.getId(), convertResult.time());
                    } catch (CorruptedFileException ex) {
                        queueService.completeWithException(fileQueueItem.getId(), ex.getMessage());
                        LOGGER.error(ex.getMessage());
                        Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                        messageService.sendMessage(
                                new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_DAMAGED_FILE, locale))
                                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                        );
                    } catch (Exception ex) {
                        if (!checker.get()) {
                            queueService.exception(fileQueueItem.getId(), ex);
                            LOGGER.error(ex.getMessage(), ex);
                            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
                            messageService.sendMessage(
                                    new HtmlMessage((long) fileQueueItem.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERSION_FAILED, locale))
                                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId())
                            );
                        }
                    }
                } else {
                    queueService.converterNotFound(fileQueueItem.getId());
                    LOGGER.debug("Candidate not found({}, {})", fileQueueItem.getUserId(), fileQueueItem.getFormat());
                }
            } finally {
                queueService.delete(fileQueueItem.getId());
                executor.complete(fileQueueItem.getId());
            }
        }

        @Override
        public int getId() {
            return fileQueueItem.getId();
        }

        @Override
        public void cancel() {
            telegramService.cancelDownloading(fileQueueItem.getFileId());
            if (canceledByUser) {
                queueService.delete(fileQueueItem.getId());
                LOGGER.debug("Canceled({}, {}, {}, {}, {})", fileQueueItem.getUserId(), fileQueueItem.getFormat(),
                        fileQueueItem.getTargetFormat(), MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()), fileQueueItem.getFileId());
            }
            executor.complete(fileQueueItem.getId());
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileQueueItem.getSize() > MemoryUtils.MB_320 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
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
                    } catch (TelegramApiRequestException ex) {
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
                    } catch (TelegramApiRequestException ex) {
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
