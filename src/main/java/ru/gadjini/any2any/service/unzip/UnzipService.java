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

@Service
public class UnzipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipService.class);

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
                    UnzipDevice unzipDevice = getCandidate(peek.getType());
                    return new UnzipTask(peek.getId(), peek.getUserId(), peek.getFile().getFileId(), peek.getFile().getSize(), peek.getType(),
                            userService.getLocaleOrDefault(peek.getUserId()), unzipDevice);
                } else {
                    return new ExtractFileTask(peek.getId(), peek.getExtractFileId(), peek.getUserId());
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
            UnzipQueueItem item = queueService.createProcessingExtractFileItem(userId, extractFileId);
            startExtracting(userId, item.getId());
            executor.execute(new ExtractFileTask(item.getId(), extractFileId, userId));
        }
    }

    public void unzip(int userId, Any2AnyFile file, Locale locale) {
        LOGGER.debug("Start unzip({}, {}, {})", file.getFormat(), file.getFileId(), userId);
        UnzipDevice unzipDevice = checkCandidate(file.getFormat(), locale);
        int id = queueService.createProcessingUnzipItem(userId, file);

        executor.execute(new UnzipTask(id, userId, file.getFileId(), file.getFileSize(), file.getFormat(), locale, unzipDevice));
    }

    public void cancel(long chatId, int jobId) {
        queueService.delete(jobId);
        executor.cancelAndComplete(jobId);
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

    private void cancelCurrentTasks(int userId) {
        List<Integer> ids = queueService.deleteByUserId(userId);
        executor.cancelAndComplete(ids);
    }

    private void startExtracting(int userId, int jobId) {
        UnzipState state = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true);
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.editMessage(
                new EditMessageText(
                        userId,
                        state.getChooseFilesMessageId(),
                        localisationService.getMessage(MessagesProperties.MESSAGE_UNZIP_PROCESSING, locale)
                ).setReplyMarkup(inlineKeyboardService.getUnzipProcessingKeyboard(jobId, locale))
        );
    }

    private void extractingCanceled(long chatId) {
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
        String message = localisationService.getMessage(
                MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
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

    private UnzipDevice checkCandidate(Format format, Locale locale) {
        for (UnzipDevice unzipDevice : unzipDevices) {
            if (unzipDevice.accept(format)) {
                return unzipDevice;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
    }

    public class ExtractFileTask implements Runnable, SmartExecutorService.Job {

        private int jobId;

        private int id;

        private int userId;

        private ExtractFileTask(int jobId, int extractFileId, int userId) {
            this.jobId = jobId;
            this.id = extractFileId;
            this.userId = userId;
        }

        @Override
        public void run() {
            UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, true);
            String fileHeader = unzipState.getFiles().get(id);
            SmartTempFile out = fileService.createTempDir();

            try {
                UnzipDevice unzipDevice = getCandidate(unzipState.getArchiveType());
                if (new File(unzipState.getArchivePath()).length() > 0) {
                    String outFilePath = unzipDevice.unzip(fileHeader, unzipState.getArchivePath(), out.getAbsolutePath());
                    SmartTempFile outFile = new SmartTempFile(new File(outFilePath), false);
                    try {
                        SendFileResult result = messageService.sendDocument(new SendDocument((long) userId, outFile.getFile()));
                        unzipState.getFilesCache().put(id, result.getFileId());
                        commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);
                        finishExtracting(userId, unzipState);
                    } finally {
                        outFile.smartDelete();
                    }
                }
            } catch (Exception ex) {
                messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                throw ex;
            } finally {
                queueService.delete(jobId);
                out.smartDelete();
                executor.complete(jobId);
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return SmartExecutorService.JobWeight.LIGHT;
        }

        private void finishExtracting(int userId, UnzipState unzipState) {
            String message = localisationService.getMessage(
                    MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                    new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
                    userService.getLocaleOrDefault(userId)
            );
            messageService.editMessage(new EditMessageText(userId, unzipState.getChooseFilesMessageId(), message)
                    .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));
        }
    }

    public class UnzipTask implements Runnable, SmartExecutorService.Job {

        private final int jobId;
        private final int userId;
        private final String fileId;
        private final int fileSize;
        private final Format format;
        private final Locale locale;
        private UnzipDevice unzipDevice;

        private UnzipTask(int jobId, int userId, String fileId, int fileSize, Format format,
                          Locale locale, UnzipDevice unzipDevice) {
            this.jobId = jobId;
            this.userId = userId;
            this.fileId = fileId;
            this.fileSize = fileSize;
            this.format = format;
            this.locale = locale;
            this.unzipDevice = unzipDevice;
        }

        @Override
        public void run() {
            SmartTempFile in = telegramService.downloadFileByFileId(fileId, format.getExt());

            try {
                UnzipState unzipState = commandStateService.getState(userId, CommandNames.UNZIP_COMMAND_NAME, false);

                if (unzipState != null) {
                    cancelCurrentTasks(userId);
                    messageService.removeInlineKeyboard(userId, unzipState.getChooseFilesMessageId());
                    new SmartTempFile(new File(unzipState.getArchivePath()), false).smartDelete();
                }
                unzipState = createState(in.getAbsolutePath(), format);
                String message = localisationService.getMessage(
                        MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                        new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
                        locale
                );
                Message sent = messageService.sendMessage(new SendMessage((long) userId, message)
                        .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));
                unzipState.setChooseFilesMessageId(sent.getMessageId());
                commandStateService.setState(userId, CommandNames.UNZIP_COMMAND_NAME, unzipState);
            } catch (Exception e) {
                in.smartDelete();
                throw e;
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
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private UnzipState createState(String zipFile, Format archiveType) {
            UnzipState unzipState = new UnzipState();
            unzipState.setArchivePath(zipFile);
            unzipState.setArchiveType(archiveType);
            List<String> zipFiles = unzipDevice.getZipFiles(zipFile);
            int i = 1;
            for (String file : zipFiles) {
                unzipState.getFiles().put(i++, file);
            }

            return unzipState;
        }
    }
}
