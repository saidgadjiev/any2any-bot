package ru.gadjini.any2any.job;

import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.model.SendDocumentContext;
import ru.gadjini.any2any.service.FileQueueService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ConverterJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterJob.class);

    private static final int LIMIT = 60;

    private FileQueueService queueService;

    private MessageService messageService;

    private Set<Any2AnyConverter<ConvertResult>> any2AnyConverters = new LinkedHashSet<>();

    @Autowired
    public ConverterJob(FileQueueService queueService, Set<Any2AnyConverter> any2AnyConvertersSet, MessageService messageService) {
        this.queueService = queueService;
        this.messageService = messageService;
        any2AnyConvertersSet.forEach(any2AnyConverter -> any2AnyConverters.add(any2AnyConverter));
    }

    @PostConstruct
    public void init() {
        applyLicenses();
    }

    @Scheduled(cron = "* * * * * *")
    public void processConverts() {
        List<FileQueueItem> items = queueService.getItems(LIMIT);
        for (FileQueueItem fileQueueItem : items) {
            Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
            if (candidate != null) {
                try (ConvertResult convertResult = candidate.convert(fileQueueItem)) {
                    logConvertFinished(fileQueueItem, convertResult.time());
                    sendResult(fileQueueItem, convertResult);
                    queueService.delete(fileQueueItem.getId());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            } else {
                LOGGER.debug("Candidate not found for: " + fileQueueItem.getFormat());
            }
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
            case FILE:
                SendDocumentContext sendDocumentContext = new SendDocumentContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId());
                messageService.sendDocument(sendDocumentContext);
                break;
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
