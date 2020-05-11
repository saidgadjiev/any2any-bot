package ru.gadjini.any2any.job;

import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.event.QueueItemCanceled;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.service.filequeue.FileQueueBusinessService;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class ConverterJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterJob.class);

    private final Map<Integer, Future<?>> processing = new ConcurrentHashMap<>();

    private ThreadPoolTaskExecutor taskExecutor;

    private FileQueueBusinessService queueService;

    private MessageService messageService;

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    @Autowired
    public ConverterJob(FileQueueBusinessService queueService, Set<Any2AnyConverter> any2AnyConvertersSet,
                        ThreadPoolTaskExecutor taskExecutor, MessageService messageService) {
        this.queueService = queueService;
        this.taskExecutor = taskExecutor;
        this.messageService = messageService;
        any2AnyConvertersSet.forEach(any2AnyConverter -> any2AnyConverters.add(any2AnyConverter));
    }

    @PostConstruct
    public void init() {
        applyLicenses();
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
        if (remainingCapacity > 0) {
            List<FileQueueItem> items = queueService.takeItems(remainingCapacity);
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
        switch (convertResult.resultType()) {
            case FILE: {
                SendFileContext sendDocumentContext = new SendFileContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId());
                messageService.sendDocument(sendDocumentContext);
                break;
            }
            case STICKER: {
                SendFileContext sendFileContext = new SendFileContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId());
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

    private void applyLicenses() {
        try {
            new License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
            new com.aspose.pdf.License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
            new com.aspose.imaging.License().setLicense(new ClassPathResource("license/license-19.lic").getInputStream());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
