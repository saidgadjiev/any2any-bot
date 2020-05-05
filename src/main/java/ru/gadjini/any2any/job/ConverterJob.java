package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.model.SendDocumentContext;
import ru.gadjini.any2any.service.FileQueueService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.converter.api.Any2AnyConverter;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.result.ConvertResult;
import ru.gadjini.any2any.service.converter.api.result.FileResult;
import ru.gadjini.any2any.util.FormatUtils;

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

    @Scheduled(cron = "* * * * * *")
    public void processConverts() {
        List<FileQueueItem> items = queueService.getItems(LIMIT);
        for (FileQueueItem fileQueueItem : items) {
            Any2AnyConverter<ConvertResult> candidate = getCandidate(fileQueueItem);
            if (candidate != null) {
                try (ConvertResult convertResult = candidate.convert(fileQueueItem, fileQueueItem.getTargetFormat())) {
                    sendResult(fileQueueItem, convertResult);
                    queueService.delete(fileQueueItem.getId());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            } else {
                LOGGER.debug("Candidate not found for: " + fileQueueItem.getMimeType());
            }
        }
    }

    private Any2AnyConverter<ConvertResult> getCandidate(FileQueueItem fileQueueItem) {
        Format format = FormatUtils.getFormat(fileQueueItem.getFileName(), fileQueueItem.getMimeType());
        for (Any2AnyConverter<ConvertResult> any2AnyConverter : any2AnyConverters) {
            if (any2AnyConverter.accept(format)) {
                return any2AnyConverter;
            }
        }

        return null;
    }

    private void sendResult(FileQueueItem fileQueueItem, ConvertResult convertResult) {
        switch (convertResult.getResultType()) {
            case FILE:
                SendDocumentContext sendDocumentContext = new SendDocumentContext(fileQueueItem.getUserId(), ((FileResult) convertResult).getFile())
                        .replyMessageId(fileQueueItem.getMessageId());
                messageService.sendDocument(sendDocumentContext);
                break;
        }
    }
}
