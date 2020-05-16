package ru.gadjini.any2any.job;

import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.configuration.SchedulerConfiguration;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.event.QueueItemCanceled;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.filequeue.FileQueueBusinessService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class ConverterJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterJob.class);

    private static final int REMAINING_SIZE = 70;

    private final Map<Integer, Future<?>> processing = new ConcurrentHashMap<>();

    private ThreadPoolTaskExecutor taskExecutor;

    private FileQueueBusinessService queueService;

    private InlineKeyboardService inlineKeyboardService;

    private UserService userService;

    private MessageService messageService;

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    @Autowired
    public ConverterJob(FileQueueBusinessService queueService, Set<Any2AnyConverter> any2AnyConvertersSet,
                        ThreadPoolTaskExecutor taskExecutor, InlineKeyboardService inlineKeyboardService,
                        UserService userService, @Qualifier("limits") MessageService messageService) {
        this.queueService = queueService;
        this.taskExecutor = taskExecutor;
        this.inlineKeyboardService = inlineKeyboardService;
        this.userService = userService;
        this.messageService = messageService;
        any2AnyConvertersSet.forEach(any2AnyConverter -> any2AnyConverters.add(any2AnyConverter));
    }

    @PostConstruct
    public void init() {
        applyLicense();
        queueService.resetProcessing();
    }

    @EventListener
    public void queueItemCanceled(QueueItemCanceled queueItemCanceled) {
        Future<?> future = processing.get(queueItemCanceled.getId());
        if (future != null && (!future.isCancelled() || !future.isDone())) {
            future.cancel(true);
        }
    }

    @Scheduled(cron = "* * * * * *")
    public void processConverts() {
        int remainingCapacity = taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity();
        int busy = SchedulerConfiguration.QUEUE_SIZE - remainingCapacity;

        if (busy < REMAINING_SIZE) {
            List<FileQueueItem> items = queueService.takeItems(REMAINING_SIZE - busy);
            for (FileQueueItem fileQueueItem : items) {
                processing.put(fileQueueItem.getId(), taskExecutor.submit(() -> {
                    try {
                        convert(fileQueueItem);
                    } finally {
                        processing.remove(fileQueueItem.getId());
                    }
                }));
            }
        }
    }

    private void convert(FileQueueItem fileQueueItem) {
        Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
        if (candidate != null) {
            try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                logConvertFinished(fileQueueItem, convertResult.time());
                sendResult(fileQueueItem, convertResult);
                queueService.complete(fileQueueItem.getId());
            } catch (Exception ex) {
                Future<?> future = processing.get(fileQueueItem.getId());
                if (future != null && future.isCancelled()) {
                    LOGGER.debug("Task canceled");
                } else {
                    queueService.exception(fileQueueItem.getId(), ex);
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        } else {
            queueService.converterNotFound(fileQueueItem.getId());
            LOGGER.debug("Candidate not found for: " + fileQueueItem.getFormat());
        }
    }

    private Any2AnyConverter<ConvertResult> getCandidate(FileQueueItem fileQueueItem) {
        for (Any2AnyConverter<ConvertResult> any2AnyConverter : any2AnyConverters) {
            if (any2AnyConverter.accept(fileQueueItem.getFormat(), fileQueueItem.getTargetFormat())) {
                return any2AnyConverter;
            }
        }

        return null;
    }

    private void sendResult(FileQueueItem fileQueueItem, ConvertResult convertResult) {
        Locale locale = userService.getLocale(fileQueueItem.getUserId());
        switch (convertResult.resultType()) {
            case FILE: {
                SendFileContext sendDocumentContext = new SendFileContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId())
                        .replyKeyboard(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                messageService.sendDocument(sendDocumentContext);
                break;
            }
            case STICKER: {
                SendFileContext sendFileContext = new SendFileContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId())
                        .replyKeyboard(inlineKeyboardService.reportKeyboard(fileQueueItem.getId(), locale));
                messageService.sendSticker(sendFileContext);
                break;
            }
        }
    }

    private void logConvertFinished(FileQueueItem fileQueueItem, long time) {
        LOGGER.debug(
                "Convert from {} to {} has taken {}. File size {} id {}",
                fileQueueItem.getFormat(),
                fileQueueItem.getTargetFormat(),
                time,
                fileQueueItem.getSize(),
                fileQueueItem.getFileId()
        );
    }

    private void applyLicense() {
        try (InputStream inputStream = new FileInputStream("license/license-19.lic")) {
            new License().setLicense(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        LOGGER.debug("Word license applied");
        try (InputStream inputStream = new FileInputStream("license/license-19.lic")) {
            new com.aspose.pdf.License().setLicense(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        LOGGER.debug("Pdf license applied");
        try (InputStream inputStream = new FileInputStream("license/license-19.lic")) {
            new com.aspose.imaging.License().setLicense(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        LOGGER.debug("Imaging license applied");
        try (InputStream inputStream = new FileInputStream("license/license-19.lic")) {
            new com.aspose.slides.License().setLicense(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        LOGGER.debug("Slides license applied");
        try (InputStream inputStream = new FileInputStream("license/license-19.lic")) {
            new com.aspose.cells.License().setLicense(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        LOGGER.debug("Cellss license applied");
    }
}
