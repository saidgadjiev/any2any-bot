package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveMessageBuilder;
import ru.gadjini.any2any.service.archive.ArchiveService;
import ru.gadjini.any2any.service.archive.ArchiveStep;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorker;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.io.File;
import java.util.Locale;

@Component
public class ArchiveQueueWorkerFactory implements QueueWorkerFactory<ArchiveQueueItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private LocalisationService localisationService;

    private MediaMessageService mediaMessageService;

    private UserService userService;

    private SmartInlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    @Autowired
    public ArchiveQueueWorkerFactory(LocalisationService localisationService, @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                                     UserService userService, SmartInlineKeyboardService inlineKeyboardService,
                                     ArchiveMessageBuilder archiveMessageBuilder) {
        this.localisationService = localisationService;
        this.mediaMessageService = mediaMessageService;
        this.userService = userService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.archiveMessageBuilder = archiveMessageBuilder;
    }

    @Override
    public QueueWorker createWorker(ArchiveQueueItem item) {
        return new ArchiveQueueWorker(item);
    }

    private Progress progressArchiveUploading(ArchiveQueueItem queueItem) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.UPLOADING, locale));

        String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.COMPLETED, locale);
        progress.setAfterProgressCompletionMessage(completionMessage);

        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }

    public class ArchiveQueueWorker implements QueueWorker {

        private final ArchiveQueueItem item;

        private ArchiveQueueWorker(ArchiveQueueItem item) {
            this.item = item;
        }

        @Override
        public void execute() {
            String size = MemoryUtils.humanReadableByteCount(item.getSize());
            LOGGER.debug("Start({}, {}, {})", item.getUserId(), size, item.getType());

            Locale locale = userService.getLocaleOrDefault(item.getUserId());

            String fileName = Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), item.getType().getExt());
            mediaMessageService.sendDocument(SendDocument.builder().chatId(String.valueOf(item.getUserId()))
                            .document(new InputFile(new File(item.getArchiveFilePath()), fileName))
                            .caption(item.getDownloadedFilesCount() < item.getFiles().size()
                                    ? localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_SIZE_EXCEEDED, locale)
                                    : null).build(),
                    progressArchiveUploading(item));

            LOGGER.debug("Finish({}, {}, {})", item.getId(), size, item.getType());
        }

        @Override
        public void unhandledException(Throwable e) {
            new SmartTempFile(new File(item.getArchiveFilePath())).smartDelete();
        }

        @Override
        public void cancel(boolean canceledByUser) {
            if (canceledByUser) {
                finish();
            }
        }

        @Override
        public void finish() {
            new SmartTempFile(new File(item.getArchiveFilePath())).smartDelete();
        }
    }
}
