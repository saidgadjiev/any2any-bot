package ru.gadjini.any2any.service.thumb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ThumbQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class ThumbService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbService.class);

    private TelegramService telegramService;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MessageService messageService;

    private ThumbQueueService thumbQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    @Autowired
    public ThumbService(TelegramService telegramService, TempFileService tempFileService, FormatService formatService,
                        @Qualifier("limits") MessageService messageService, ThumbQueueService thumbQueueService,
                        LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                        CommandStateService commandStateService, UserService userService) {
        this.telegramService = telegramService;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.thumbQueueService = thumbQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        thumbQueueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("thumbTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void reject(SmartExecutorService.Job job) {
        thumbQueueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({})", job.getWeight());
    }

    public ThumbTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            ThumbQueueItem peek = thumbQueueService.poll(weight);

            if (peek != null) {
                return new ThumbTask(peek);
            }
            return null;
        }
    }

    public void setThumb(int userId, ThumbState thumbState) {
        ThumbQueueItem item = thumbQueueService.createProcessingItem(userId, thumbState);
        sendStartRenamingMessage(item.getId(), userId);
        executor.execute(new ThumbTask(item));
    }

    public void removeAndCancelCurrentTasks(long chatId) {
        ThumbState thumbState = commandStateService.getState(chatId, CommandNames.THUMB_COMMAND, false);
        if (thumbState != null) {
            List<Integer> ids = thumbQueueService.deleteByUserId((int) chatId);
            executor.cancelAndComplete(ids, false);
            commandStateService.deleteState(chatId, CommandNames.THUMB_COMMAND);
        }
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!thumbQueueService.exists(jobId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, userService.getLocaleOrDefault((int) chatId)),
                    true
            ));
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))
            ));
            if (!executor.cancelAndComplete(jobId, true)) {
                thumbQueueService.delete(jobId);
                commandStateService.deleteState(chatId, CommandNames.THUMB_COMMAND);
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void leave(long chatId) {
        List<Integer> ids = thumbQueueService.deleteByUserId((int) chatId);
        if (ids.size() > 0) {
            LOGGER.debug("Leave({}, {})", chatId, ids.size());
        }
        executor.cancelAndComplete(ids, false);
        commandStateService.deleteState(chatId, CommandNames.THUMB_COMMAND);
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<ThumbQueueItem> tasks = thumbQueueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (ThumbQueueItem item : tasks) {
            executor.execute(new ThumbTask(item));
        }
    }

    private void sendStartRenamingMessage(int jobId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(
                new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_PROCESSING, locale))
                        .setReplyMarkup(inlineKeyboardService.getThumbProcessingKeyboard(jobId, locale)));
    }

    public final class ThumbTask implements SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(ThumbTask.class);

        private static final String TAG = "thumb";

        private volatile Supplier<Boolean> checker;

        private volatile boolean canceledByUser;

        private ThumbQueueItem item;

        private volatile SmartTempFile file;

        private volatile SmartTempFile thumb;

        private ThumbTask(ThumbQueueItem queueItem) {
            this.item = queueItem;
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(item.getFile().getSize());
            LOGGER.debug("Start({}, {}, {}, {})", item.getUserId(), size, item.getFile().getFileId(), item.getThumb().getFileId());

            try {
                String ext = formatService.getExt(item.getFile().getFileName(), item.getFile().getMimeType());
                file = tempFileService.createTempFile(TAG, ext);
                telegramService.downloadFileByFileId(item.getFile().getFileId(), file);

                thumb = tempFileService.createTempFile(TAG, Format.JPG.getExt());
                telegramService.downloadFileByFileId(item.getThumb().getFileId(), thumb);

                messageService.sendDocument(new SendDocument((long) item.getUserId(), item.getFile().getFileName(), file.getFile()).setThumb(thumb.getAbsolutePath()));

                LOGGER.debug("Finish({}, {}, {}, {})", item.getUserId(), size, item.getFile().getFileId(), item.getThumb().getFileId());
            } catch (Exception ex) {
                if (!checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(item.getUserId(), userService.getLocaleOrDefault(item.getUserId()));
                }
            } finally {
                if (!checker.get()) {
                    executor.complete(item.getId());
                    thumbQueueService.delete(item.getId());
                    commandStateService.deleteState(item.getUserId(), CommandNames.THUMB_COMMAND);
                    if (file != null) {
                        file.smartDelete();
                    }
                    if (thumb != null) {
                        thumb.smartDelete();
                    }
                }
            }
        }

        @Override
        public int getId() {
            return item.getId();
        }

        @Override
        public void cancel() {
            if (!telegramService.cancelDownloading(item.getFile().getFileId()) && file != null) {
                file.smartDelete();
            }
            if (!telegramService.cancelDownloading(item.getThumb().getFileId()) && thumb != null) {
                thumb.smartDelete();
            }
            if (canceledByUser) {
                thumbQueueService.deleteWithReturning(item.getId());
                LOGGER.debug("Canceled by user({}, {})", item.getUserId(), MemoryUtils.humanReadableByteCount(item.getFile().getSize()));
            }

            commandStateService.deleteState(item.getUserId(), CommandNames.THUMB_COMMAND);
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
            return item.getFile().getSize() > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }
    }
}
