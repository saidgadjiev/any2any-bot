package ru.gadjini.any2any.service.unzip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.unzip.UnzipQueueService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class UnzipService {

    private Set<UnzipDevice> unzipDevices;

    private LocalisationService localisationService;

    private SmartExecutorService executor;

    private MessageService messageService;

    private TelegramService telegramService;

    private TempFileService fileService;

    private UnzipQueueService queueService;

    private UserService userService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private UnzipMessageBuilder messageBuilder;

    @Autowired
    public UnzipService(Set<UnzipDevice> unzipDevices, LocalisationService localisationService,
                        @Qualifier("limits") MessageService messageService,
                        TelegramService telegramService, TempFileService fileService,
                        UnzipQueueService queueService, UserService userService,
                        CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService,
                        UnzipMessageBuilder messageBuilder) {
        this.unzipDevices = unzipDevices;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.queueService = queueService;
        this.userService = userService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageBuilder = messageBuilder;
    }

    @PostConstruct
    public void init() {
        queueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("unzipTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectTask(SmartExecutorService.Job unzipTask) {
        if (unzipTask instanceof UnzipTask) {
            queueService.setWaiting(unzipTask.getId(), ((UnzipTask) unzipTask).messageId);
        } else {
            queueService.setWaiting(unzipTask.getId());
        }
    }

    public Runnable getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            UnzipQueueItem peek = queueService.poll(weight);

            if (peek != null) {
                if (peek.getItemType() == UnzipQueueItem.ItemType.UNZIP) {
                    return new UnzipTask(peek);
                } else {
                    return new ExtractFileTask(peek);
                }
            }

            return null;
        }
    }

    public void extractFile(int userId, int extractFileId, String queryId) {
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true);
        if (unzipState.getFilesCache().containsKey(extractFileId)) {
            messageService.sendAnswerCallbackQuery(
                    new AnswerCallbackQuery(
                            queryId,
                            localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING_ANSWER, userService.getLocaleOrDefault(userId))
                    )
            );
            messageService.sendDocument(new SendDocument((long) userId, unzipState.getFilesCache().get(extractFileId)));
        } else {
            UnzipQueueItem item = queueService.createProcessingExtractFileItem(userId, extractFileId, unzipState.getFiles().get(extractFileId).getSize());
            startExtracting(userId, item.getId());
            executor.execute(new ExtractFileTask(item));
        }
    }

    public void unzip(int userId, Any2AnyFile file, Locale locale) {
        checkCandidate(file.getFormat(), locale);
        UnzipQueueItem queueItem = queueService.createProcessingUnzipItem(userId, file);
        int messageId = startUnzipping(userId, queueItem.getId(), locale);
        queueItem.setMessageId(messageId);

        executor.execute(new UnzipTask(queueItem));
    }

    public void cancelUnzip(long chatId, int messageId, int jobId) {
        queueService.delete(jobId);
        executor.cancelAndComplete(jobId, true);
        unzipCancelled(chatId, messageId);
    }

    public void cancelExtractFile(long chatId, int jobId) {
        queueService.delete(jobId);
        executor.cancelAndComplete(jobId, true);
        extractingCanceled(chatId);
    }

    public void leave(long chatId) {
        cancelCurrentTasks((int) chatId);
        UnzipState state = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
        if (state != null) {
            messageService.removeInlineKeyboard(chatId, state.getChooseFilesMessageId());
            commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
        }
    }

    private int startUnzipping(int userId, int jobId, Locale locale) {
        return messageService.sendMessage(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_PROCESSING, locale))
                .setReplyMarkup(inlineKeyboardService.getUnzipProcessingKeyboard(jobId, locale))).getMessageId();
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<UnzipQueueItem> tasks = queueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (UnzipQueueItem item : tasks) {
            if (item.getItemType() == UnzipQueueItem.ItemType.UNZIP) {
                executor.execute(new UnzipTask(item));
            } else {
                executor.execute(new ExtractFileTask(item));
            }
        }
    }

    private void cancelCurrentTasks(int userId) {
        List<Integer> ids = queueService.deleteByUserId(userId);
        executor.cancelAndComplete(ids, false);
    }

    private void startExtracting(int userId, int jobId) {
        UnzipState state = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true);
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.editMessage(
                new EditMessageText(
                        userId,
                        state.getChooseFilesMessageId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING, locale)
                ).setReplyMarkup(inlineKeyboardService.getExtractFileProcessingKeyboard(jobId, locale))
        );
    }

    private void unzipCancelled(long chatId, int messageId) {
        messageService.removeInlineKeyboard(chatId, messageId);
    }

    private void extractingCanceled(long chatId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
        String message = localisationService.getMessage(
                MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                userService.getLocaleOrDefault((int) chatId));
        messageService.editMessage(new EditMessageText(chatId, unzipState.getChooseFilesMessageId(), message)
                .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));
    }

    private UnzipDevice getCandidate(Format format) {
        for (UnzipDevice unzipDevice : unzipDevices) {
            if (unzipDevice.accept(format)) {
                return unzipDevice;
            }
        }

        throw new IllegalArgumentException("Candidate not found for " + format + ". Wtf?");
    }

    private void checkCandidate(Format format, Locale locale) {
        for (UnzipDevice unzipDevice : unzipDevices) {
            if (unzipDevice.accept(format)) {
                return;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public class ExtractFileTask implements Runnable, SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(UnzipTask.class);

        private int jobId;

        private int id;

        private int userId;

        private long fileSize;

        private volatile Supplier<Boolean> checker;

        private volatile boolean autoCancel;

        private volatile SmartTempFile out;

        private ExtractFileTask(UnzipQueueItem item) {
            this.jobId = item.getId();
            this.id = item.getExtractFileId();
            this.userId = item.getUserId();
            this.fileSize = item.getExtractFileSize();
        }

        @Override
        public void run() {
            String size = null;

            try {
                UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true);
                ZipFileHeader fileHeader = unzipState.getFiles().get(id);
                size = MemoryUtils.humanReadableByteCount(fileHeader.getSize());
                LOGGER.debug("Start({}, {})", userId, size);
                out = fileService.createTempDir();

                UnzipDevice unzipDevice = getCandidate(unzipState.getArchiveType());
                String outFilePath = unzipDevice.unzip(fileHeader.getPath(), unzipState.getArchivePath(), out.getAbsolutePath());
                SmartTempFile outFile = new SmartTempFile(new File(outFilePath), false);

                SendFileResult result = messageService.sendDocument(new SendDocument((long) userId, outFile.getFile()));
                unzipState.getFilesCache().put(id, result.getFileId());
                commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);
                finishExtracting(userId, unzipState);

                LOGGER.debug("Finish({}, {})", userId, size);
            } catch (Exception ex) {
                if (checker.get()) {
                    if (!autoCancel) {
                        LOGGER.debug("Canceled by user ({}, {})", userId, size);
                    } else {
                        LOGGER.debug("Canceled({}, {})", userId, size);
                    }
                } else {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                cleanup();
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            autoCancel = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void cleanup() {
            if (!autoCancel) {
                queueService.delete(jobId);
            }
            executor.complete(jobId);
            if (out != null) {
                out.smartDelete();
            }
        }

        private void finishExtracting(int userId, UnzipState unzipState) {
            String message = localisationService.getMessage(
                    MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                    new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                    userService.getLocaleOrDefault(userId)
            );
            messageService.editMessage(new EditMessageText(userId, unzipState.getChooseFilesMessageId(), message)
                    .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));
        }
    }

    public class UnzipTask implements Runnable, SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(UnzipTask.class);

        private final int jobId;
        private final int userId;
        private final String fileId;
        private final int fileSize;
        private final Format format;
        private Integer messageId;
        private UnzipDevice unzipDevice;
        private volatile Supplier<Boolean> checker;
        private volatile boolean autoCancel;

        private UnzipTask(UnzipQueueItem item) {
            this.jobId = item.getId();
            this.userId = item.getUserId();
            this.fileId = item.getFile().getFileId();
            this.fileSize = item.getFile().getSize();
            this.format = item.getType();
            this.unzipDevice = getCandidate(item.getType());
            this.messageId = item.getMessageId();
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {}, {})", userId, size, format, fileId);

            SmartTempFile in = null;
            try {
                in = telegramService.downloadFileByFileId(fileId, format.getExt());
                UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false);

                if (unzipState != null) {
                    cancelCurrentTasks(userId);
                    messageService.removeInlineKeyboard(userId, unzipState.getChooseFilesMessageId());
                    new SmartTempFile(new File(unzipState.getArchivePath()), false).smartDelete();
                }
                unzipState = createState(in.getAbsolutePath(), format);
                String message = localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                        userService.getLocaleOrDefault(userId)
                );
                messageService.removeInlineKeyboard(userId, messageId);
                Message sent = messageService.sendMessage(new SendMessage((long) userId, message)
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));
                unzipState.setChooseFilesMessageId(sent.getMessageId());
                commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);

                LOGGER.debug("Finish({}, {}, {})", userId, size, format);
            } catch (Exception e) {
                if (checker.get()) {
                    if (!autoCancel) {
                        LOGGER.debug("Canceled by user ({}, {})", userId, size);
                    } else {
                        LOGGER.debug("Canceled({}, {})", userId, size);
                    }
                } else {
                    LOGGER.error(e.getMessage(), e);
                    if (in != null) {
                        in.smartDelete();
                    }
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                cleanup();
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void cancel() {
            telegramService.cancelDownloading(fileId);
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.autoCancel = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void cleanup() {
            executor.complete(jobId);
            if (!autoCancel) {
                queueService.delete(jobId);
            }
        }

        private UnzipState createState(String zipFile, Format archiveType) {
            UnzipState unzipState = new UnzipState();
            unzipState.setArchivePath(zipFile);
            unzipState.setArchiveType(archiveType);
            List<ZipFileHeader> zipFiles = unzipDevice.getZipFiles(zipFile);
            int i = 1;
            for (ZipFileHeader file : zipFiles) {
                unzipState.getFiles().put(i++, file);
            }

            return unzipState;
        }
    }
}
