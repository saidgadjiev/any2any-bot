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

    private final Logger LOGGER = LoggerFactory.getLogger(UnzipService.class);

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
        queueService.setWaiting(unzipTask.getId());
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

    public void extractFile(int userId, int messageId, int unzipJobId, int extractFileId, String queryId) {
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false);
        if (unzipState == null) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboard(userId, messageId);
        } else if (unzipState.getUnzipJobId() != unzipJobId) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboard(userId, messageId);
        } else {
            if (unzipState.getFilesCache().containsKey(extractFileId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackQuery(
                                queryId,
                                localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING_ANSWER, userService.getLocaleOrDefault(userId))
                        )
                );
                messageService.sendDocument(new SendDocument((long) userId, unzipState.getFilesCache().get(extractFileId)));
            } else {
                UnzipQueueItem item = queueService.createProcessingExtractFileItem(userId, messageId,
                        extractFileId, unzipState.getFiles().get(extractFileId).getSize());
                sendStartExtractingMessage(userId, messageId, item.getId());
                executor.execute(new ExtractFileTask(item));
            }
        }
    }

    public void unzip(int userId, Any2AnyFile file, Locale locale) {
        checkCandidate(file.getFormat(), locale);
        UnzipQueueItem queueItem = queueService.createProcessingUnzipItem(userId, file);
        sendStartUnzippingMessage(userId, queueItem.getId(), locale);

        executor.execute(new UnzipTask(queueItem));
    }

    public void removeAndCancelCurrentTasks(long chatId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);

        if (unzipState != null) {
            LOGGER.debug("Remove previous state({})", chatId);
            List<Integer> ids = queueService.deleteByUserId((int) chatId);
            executor.cancelAndComplete(ids, true);
            new SmartTempFile(new File(unzipState.getArchivePath()), false).smartDelete();
            commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
        }
    }

    public void cancelUnzip(long chatId, int messageId, String queryId, int jobId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
        if (unzipState == null || unzipState.getUnzipJobId() != jobId) {
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
                queueService.delete(jobId);
                commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void cancelExtractFile(long chatId, int messageId, String queryId, int jobId) {
        if (!queueService.exists(jobId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, userService.getLocaleOrDefault((int) chatId)),
                    true
            ));
            messageService.editMessage(new EditMessageText(
                    chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))
            ));
            UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
            if (unzipState != null) {
                String message = localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                        userService.getLocaleOrDefault((int) chatId));
                messageService.editMessage(new EditMessageText(chatId, messageId, message)
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), unzipState.getUnzipJobId())));
            }

            if (!executor.cancelAndComplete(jobId, true)) {
                queueService.delete(jobId);
            }
        }
    }

    public void leave(long chatId) {
        List<Integer> ids = queueService.deleteByUserId((int) chatId);
        executor.cancelAndComplete(ids, true);

        if (ids.size() > 0) {
            LOGGER.debug("Leave({}, {})", chatId, ids.size());
        }
        commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
    }

    private void sendStartUnzippingMessage(int userId, int jobId, Locale locale) {
        messageService.sendMessage(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_PROCESSING, locale))
                .setReplyMarkup(inlineKeyboardService.getUnzipProcessingKeyboard(jobId, locale)));
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

    private void sendStartExtractingMessage(int userId, int messageId, int jobId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.editMessage(
                new EditMessageText(
                        userId,
                        messageId,
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING, locale)
                ).setReplyMarkup(inlineKeyboardService.getExtractFileProcessingKeyboard(jobId, locale))
        );
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

        private final Logger LOGGER = LoggerFactory.getLogger(ExtractFileTask.class);

        private int jobId;

        private int id;

        private int userId;

        private int messageId;

        private long fileSize;

        private volatile Supplier<Boolean> checker;

        private volatile boolean canceledByUser;

        private volatile SmartTempFile out;

        private ExtractFileTask(UnzipQueueItem item) {
            this.jobId = item.getId();
            this.id = item.getExtractFileId();
            this.userId = item.getUserId();
            this.fileSize = item.getExtractFileSize();
            this.messageId = item.getMessageId();
        }

        @Override
        public void run() {
            String size;

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
                if (!checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                executor.complete(jobId);
                queueService.delete(jobId);
                commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
                if (out != null) {
                    out.smartDelete();
                }
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
            this.canceledByUser = !canceledByUser;
        }

        @Override
        public void cancel() {
            if (canceledByUser) {
                queueService.deleteWithReturning(jobId);
                LOGGER.debug("Extracting canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
            commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);
            if (out != null) {
                out.smartDelete();
            }
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void finishExtracting(int userId, UnzipState unzipState) {
            String message = localisationService.getMessage(
                    MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                    new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                    userService.getLocaleOrDefault(userId)
            );
            messageService.editMessage(new EditMessageText(userId, messageId, message)
                    .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), unzipState.getUnzipJobId())));
        }
    }

    public class UnzipTask implements Runnable, SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(UnzipTask.class);

        private final int jobId;
        private final int userId;
        private final String fileId;
        private final int fileSize;
        private final Format format;
        private UnzipDevice unzipDevice;
        private volatile Supplier<Boolean> checker;
        private volatile boolean canceledByUser;
        private volatile SmartTempFile in;

        private UnzipTask(UnzipQueueItem item) {
            this.jobId = item.getId();
            this.userId = item.getUserId();
            this.fileId = item.getFile().getFileId();
            this.fileSize = item.getFile().getSize();
            this.format = item.getType();
            this.unzipDevice = getCandidate(item.getType());
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {}, {})", userId, size, format, fileId);

            try {
                in = telegramService.downloadFileByFileId(fileId, format.getExt());
                UnzipState unzipState = initAndGetState(in.getAbsolutePath());
                if (unzipState == null) {
                    return;
                }
                String message = localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values().stream().map(ZipFileHeader::getPath).collect(Collectors.toList()))},
                        userService.getLocaleOrDefault(userId)
                );
                messageService.sendMessage(new SendMessage((long) userId, message)
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), jobId)));
                commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);

                LOGGER.debug("Finish({}, {}, {})", userId, size, format);
            } catch (Exception e) {
                if (!checker.get()) {
                    LOGGER.error(e.getMessage(), e);
                    if (in != null) {
                        in.smartDelete();
                    }
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                executor.complete(jobId);
                queueService.delete(jobId);
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

            if (canceledByUser) {
                queueService.deleteWithReturning(jobId);
                LOGGER.debug("Unzip canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
            commandStateService.deleteState(userId, CommandNames.UNZIP_COMMAND_NAME);
            if (in != null) {
                in.smartDelete();
            }
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private UnzipState initAndGetState(String zipFile) {
            UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false);
            if (unzipState == null) {
                return null;
            }
            unzipState.setArchivePath(zipFile);
            unzipState.setUnzipJobId(jobId);
            List<ZipFileHeader> zipFiles = unzipDevice.getZipFiles(zipFile);
            int i = 1;
            for (ZipFileHeader file : zipFiles) {
                unzipState.getFiles().put(i++, file);
            }

            return unzipState;
        }
    }
}
