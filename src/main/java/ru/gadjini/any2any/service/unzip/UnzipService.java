package ru.gadjini.any2any.service.unzip;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.exception.ProcessException;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.exception.botapi.TelegramApiException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.ZipFileHeader;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.file.FileManager;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.unzip.UnzipQueueService;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class UnzipService {

    private final Logger LOGGER = LoggerFactory.getLogger(UnzipService.class);

    private Set<UnzipDevice> unzipDevices;

    private LocalisationService localisationService;

    private SmartExecutorService executor;

    private MessageService messageService;

    private FileManager fileManager;

    private TempFileService fileService;

    private UnzipQueueService queueService;

    private UserService userService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    private UnzipMessageBuilder messageBuilder;

    @Autowired
    public UnzipService(Set<UnzipDevice> unzipDevices, LocalisationService localisationService,
                        @Qualifier("limits") MessageService messageService,
                        FileManager fileManager, TempFileService fileService,
                        UnzipQueueService queueService, UserService userService,
                        CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService,
                        UnzipMessageBuilder messageBuilder) {
        this.unzipDevices = unzipDevices;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.fileManager = fileManager;
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
        LOGGER.debug("Rejected({})", unzipTask.getWeight());
    }

    public Runnable getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            UnzipQueueItem peek = queueService.poll(weight);

            if (peek != null) {
                if (peek.getItemType() == UnzipQueueItem.ItemType.UNZIP) {
                    return new UnzipTask(peek);
                } else if (peek.getItemType() == UnzipQueueItem.ItemType.EXTRACT_FILE) {
                    return new ExtractFileTask(peek);
                } else {
                    return new ExtractAllTask(peek);
                }
            }

            return null;
        }
    }

    public void extractAll(int userId, int messageId, int unzipJobId, String queryId) {
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
        if (unzipState == null) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboardAsync(userId, messageId);
        } else if (unzipState.getUnzipJobId() != unzipJobId) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboardAsync(userId, messageId);
        } else {
            sendStartExtractingAllMessage(userId, messageId, unzipJobId);
            UnzipQueueItem item = queueService.createProcessingExtractAllItem(userId, messageId,
                    unzipState.getFiles().values().stream().map(ZipFileHeader::getSize).mapToLong(i -> i).sum());
            sendStartExtractingAllMessage(userId, messageId, item.getId());
            executor.execute(new ExtractAllTask(item));
        }
    }

    public void extractFile(int userId, int messageId, int unzipJobId, int extractFileId, String queryId) {
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
        if (unzipState == null) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboardAsync(userId, messageId);
        } else if (unzipState.getUnzipJobId() != unzipJobId) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboardAsync(userId, messageId);
        } else {
            if (unzipState.getFilesCache().containsKey(extractFileId)) {
                messageService.sendAnswerCallbackQuery(
                        new AnswerCallbackQuery(
                                queryId,
                                localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING_ANSWER, userService.getLocaleOrDefault(userId))
                        )
                );
                messageService.sendFileAsync(userId, unzipState.getFilesCache().get(extractFileId));
            } else {
                UnzipQueueItem item = queueService.createProcessingExtractFileItem(userId, messageId,
                        extractFileId, unzipState.getFiles().get(extractFileId).getSize());
                sendStartExtractingFileMessage(userId, messageId, item.getId());
                executor.execute(new ExtractFileTask(item));
            }
        }
    }

    public void nextOrPrev(String queryId, long chatId, int userId, int messageId, int prevLimit, int offset) {
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
        if (unzipState == null) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_IMPOSSIBLE, userService.getLocaleOrDefault(userId)),
                    true
            ));
            messageService.removeInlineKeyboardAsync(userId, messageId);
        } else {
            Locale locale = userService.getLocaleOrDefault(userId);
            UnzipMessageBuilder.FilesMessage filesList = messageBuilder.getFilesList(unzipState.getFiles(), 0, offset, locale);
            InlineKeyboardMarkup filesListKeyboard = inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(),
                    filesList.getLimit(), prevLimit, offset, unzipState.getUnzipJobId(), locale);

            try {
                messageService.editMessageAsync(new EditMessageText(chatId, messageId, filesList.getMessage())
                        .setThrowEx(true)
                        .setReplyMarkup(filesListKeyboard));
            } catch (Exception e) {
                messageService.sendMessageAsync(new SendMessage((long) userId, filesList.getMessage())
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), filesList.getLimit(), 0, filesList.getOffset(), unzipState.getUnzipJobId(), locale)));
                if (!(e instanceof TelegramApiException)) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public void unzip(int userId, Any2AnyFile file, Locale locale) {
        checkCandidate(file.getFormat(), locale);
        UnzipQueueItem queueItem = queueService.createProcessingUnzipItem(userId, file);
        UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true, UnzipState.class);
        unzipState.setUnzipJobId(queueItem.getId());
        commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);

        sendStartUnzippingMessage(userId, queueItem.getId(), locale, messageId -> {
            queueItem.setMessageId(messageId);
            queueService.setMessageId(queueItem.getId(), messageId);

            executor.execute(new UnzipTask(queueItem));
        });
    }

    public void removeAndCancelCurrentTasks(long chatId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);

        if (unzipState != null) {
            LOGGER.debug("Remove previous state({})", chatId);
            List<Integer> ids = queueService.deleteByUserId((int) chatId);
            executor.cancelAndComplete(ids, true);
            if (StringUtils.isNotBlank(unzipState.getArchivePath())) {
                new SmartTempFile(new File(unzipState.getArchivePath())).smartDelete();
            }
            commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
        }
    }

    public void cancelUnzip(long chatId, int messageId, String queryId, int jobId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
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
                if (StringUtils.isNotBlank(unzipState.getArchivePath())) {
                    new SmartTempFile(new File(unzipState.getArchivePath())).smartDelete();
                }
            }
        }
        messageService.editMessageAsync(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void cancelExtractFile(long chatId, int messageId, String queryId, int jobId) {
        if (!queueService.exists(jobId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, userService.getLocaleOrDefault((int) chatId)),
                    true
            ));
            messageService.editMessageAsync(new EditMessageText(
                    chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))
            ));
            UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
            if (unzipState != null) {
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                String message = localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
                        locale
                );
                messageService.editMessageAsync(new EditMessageText(chatId, messageId, message)
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), unzipState.getUnzipJobId(), locale)));
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
        UnzipState state = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
        if (state != null) {
            if (StringUtils.isNotBlank(state.getArchivePath())) {
                new SmartTempFile(new File(state.getArchivePath())).smartDelete();
            }
            commandStateService.deleteState(chatId, CommandNames.UNZIP_COMMAND_NAME);
        }
    }

    private void sendStartUnzippingMessage(int userId, int jobId, Locale locale, Consumer<Integer> consumer) {
        messageService.sendMessageAsync(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_PROCESSING, locale))
                .setReplyMarkup(inlineKeyboardService.getUnzipProcessingKeyboard(jobId, locale)), message -> consumer.accept(message.getMessageId()));
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

    private void sendStartExtractingAllMessage(int userId, int messageId, int jobId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.editMessageAsync(
                new EditMessageText(
                        userId,
                        messageId,
                        localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_ALL_PROCESSING, locale)
                ).setReplyMarkup(inlineKeyboardService.getExtractFileProcessingKeyboard(jobId, locale))
        );
    }

    private void sendStartExtractingFileMessage(int userId, int messageId, int jobId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.editMessageAsync(
                new EditMessageText(
                        userId,
                        messageId,
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING, locale)
                ).setReplyMarkup(inlineKeyboardService.getExtractFileProcessingKeyboard(jobId, locale))
        );
    }

    private void finishExtracting(int userId, int messageId, UnzipState unzipState) {
        Locale locale = userService.getLocaleOrDefault(userId);
        String message = localisationService.getMessage(
                MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
                locale
        );
        messageService.editMessageAsync(new EditMessageText(userId, messageId, message)
                .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), unzipState.getUnzipJobId(), locale)));
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

        LOGGER.warn("Candidate not found({})", format);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public class ExtractAllTask implements SmartExecutorService.Job {

        public static final String TAG = "extractall";

        private UnzipQueueItem item;

        private Queue<SmartTempFile> files = new LinkedBlockingQueue<>();

        private volatile Supplier<Boolean> checker;

        private volatile boolean canceledByUser;

        private ExtractAllTask(UnzipQueueItem item) {
            this.item = item;
        }

        @Override
        public int getId() {
            return item.getId();
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return item.getExtractFileSize() > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(item.getExtractFileSize());
            LOGGER.debug("Start extract all({}, {})", item.getUserId(), size);
            UnzipState unzipState = commandStateService.getState(item.getUserId(), CommandNames.UNZIP_COMMAND_NAME, true, UnzipState.class);

            try {
                UnzipDevice unzipDevice = getCandidate(unzipState.getArchiveType());

                for (Map.Entry<Integer, ZipFileHeader> entry : unzipState.getFiles().entrySet()) {
                    if (unzipState.getFilesCache().containsKey(entry.getKey())) {
                        messageService.sendFileAsync(item.getUserId(), unzipState.getFilesCache().get(entry.getKey()));
                    } else {
                        SmartTempFile file = fileService.createTempFile(item.getUserId(), TAG, FilenameUtils.getExtension(entry.getValue().getPath()));
                        files.add(file);
                        unzipDevice.unzip(entry.getValue().getPath(), unzipState.getArchivePath(), file.getAbsolutePath());

                        String fileName = FilenameUtils.getName(entry.getValue().getPath());
                        messageService.sendDocumentAsync(new SendDocument((long) item.getUserId(),
                                fileName, file.getFile()).setCaption(fileName), result -> {
                            try {
                                if (result != null) {
                                    unzipState.getFilesCache().put(entry.getKey(), result.getFileId());
                                    commandStateService.setState(item.getUserId(), CommandNames.UNZIP_COMMAND_NAME, unzipState);
                                }
                            } finally {
                                file.smartDelete();
                            }
                        });
                    }
                }
                LOGGER.debug("Finish extract all({}, {})", item.getUserId(), size);
            } catch (Exception ex) {
                if (!checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);

                    files.forEach(SmartTempFile::smartDelete);
                    Locale locale = userService.getLocaleOrDefault(item.getUserId());
                    if (ex instanceof ProcessException) {
                        messageService.sendMessageAsync(new SendMessage((long) item.getUserId(), localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_ERROR, locale)));
                    } else {
                        messageService.sendErrorMessage(item.getUserId(), locale);
                    }
                }
            } finally {
                if (!checker.get()) {
                    finishExtracting(item.getUserId(), item.getMessageId(), unzipState);
                    executor.complete(item.getId());
                    queueService.delete(item.getId());
                }
            }
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
        public void cancel() {
            if (canceledByUser) {
                queueService.delete(item.getId());
                LOGGER.debug("Extracting canceled by user({}, {})", item.getUserId(), MemoryUtils.humanReadableByteCount(item.getExtractFileSize()));
            }
            files.forEach(SmartTempFile::smartDelete);
        }
    }

    public class ExtractFileTask implements SmartExecutorService.Job {

        public static final String TAG = "extractfile";

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

            UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true, UnzipState.class);
            try {
                ZipFileHeader fileHeader = unzipState.getFiles().get(id);
                size = MemoryUtils.humanReadableByteCount(fileHeader.getSize());
                LOGGER.debug("Start({}, {})", userId, size);

                UnzipDevice unzipDevice = getCandidate(unzipState.getArchiveType());
                out = fileService.createTempFile(userId, TAG, FilenameUtils.getExtension(fileHeader.getPath()));
                unzipDevice.unzip(fileHeader.getPath(), unzipState.getArchivePath(), out.getAbsolutePath());

                String fileName = FilenameUtils.getName(fileHeader.getPath());
                messageService.sendDocumentAsync(new SendDocument((long) userId, fileName, out.getFile())
                        .setCaption(fileName), result -> {
                    try {
                        if (result != null) {
                            unzipState.getFilesCache().put(id, result.getFileId());
                            commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);
                        }
                        LOGGER.debug("Finish({}, {})", userId, size);
                    } finally {
                        if (out != null) {
                            out.smartDelete();
                        }
                    }
                });
            } catch (Exception ex) {
                if (!checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    if (out != null) {
                        out.smartDelete();
                    }
                    Locale locale = userService.getLocaleOrDefault(userId);
                    if (ex instanceof ProcessException) {
                        messageService.sendMessageAsync(new SendMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACT_FILE_ERROR, locale)));
                    } else {
                        messageService.sendErrorMessage(userId, locale);
                    }
                }
            } finally {
                if (!checker.get()) {
                    finishExtracting(userId, messageId, unzipState);
                    executor.complete(jobId);
                    queueService.delete(jobId);
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
            this.canceledByUser = canceledByUser;
        }

        @Override
        public void cancel() {
            if (canceledByUser) {
                queueService.delete(jobId);
                LOGGER.debug("Extracting canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
            if (out != null) {
                out.smartDelete();
            }
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }
    }

    public class UnzipTask implements SmartExecutorService.Job {

        public static final String TAG = "unzip";

        private final Logger LOGGER = LoggerFactory.getLogger(UnzipTask.class);

        private final int jobId;
        private final int userId;
        private final String fileId;
        private final int fileSize;
        private final Format format;
        private final int messageId;
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
            this.messageId = item.getMessageId();
        }

        @Override
        public void run() {
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {}, {})", userId, size, format, fileId);

            try {
                in = fileService.createTempFile(userId, fileId, TAG, format.getExt());
                fileManager.downloadFileByFileId(userId, fileId, in);
                UnzipState unzipState = initAndGetState(in.getAbsolutePath());
                if (unzipState == null) {
                    return;
                }
                Locale locale = userService.getLocaleOrDefault(userId);
                UnzipMessageBuilder.FilesMessage filesList = messageBuilder.getFilesList(unzipState.getFiles(), 0, 0, locale);

                try {
                    messageService.editMessageAsync(new EditMessageText((long) userId, messageId, filesList.getMessage())
                            .setThrowEx(true)
                            .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), filesList.getLimit(), 0, filesList.getOffset(), jobId, locale)));
                } catch (Exception e) {
                    messageService.sendMessageAsync(new SendMessage((long) userId, filesList.getMessage())
                            .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds(), filesList.getLimit(), 0, filesList.getOffset(), jobId, locale)));
                    if (!(e instanceof TelegramApiException)) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
                commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);

                LOGGER.debug("Finish({}, {}, {})", userId, size, format);
            } catch (Exception e) {
                if (!checker.get()) {
                    LOGGER.error(e.getMessage(), e);
                    if (in != null) {
                        in.smartDelete();
                    }
                    commandStateService.deleteState(userId, CommandNames.UNZIP_COMMAND_NAME);
                    Locale locale = userService.getLocaleOrDefault(userId);
                    if (e instanceof ProcessException) {
                        messageService.sendMessageAsync(new SendMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_ERROR, locale)));
                    } else {
                        messageService.sendErrorMessage(userId, locale);
                    }
                }
            } finally {
                if (!checker.get()) {
                    executor.complete(jobId);
                    queueService.delete(jobId);
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
        public void cancel() {
            if (!fileManager.cancelDownloading(fileId) && in != null) {
                in.smartDelete();
            }
            if (canceledByUser) {
                queueService.delete(jobId);
                LOGGER.debug("Unzip canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
            commandStateService.deleteState(userId, CommandNames.UNZIP_COMMAND_NAME);
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private UnzipState initAndGetState(String zipFile) {
            UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false, UnzipState.class);
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
