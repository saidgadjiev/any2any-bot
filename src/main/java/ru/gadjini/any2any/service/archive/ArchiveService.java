package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
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
import ru.gadjini.any2any.service.queue.archive.ArchiveQueueService;
import ru.gadjini.any2any.utils.Any2AnyFileNameUtils;
import ru.gadjini.any2any.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    private SmartExecutorService executor;

    private TelegramService telegramService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private ArchiveQueueService archiveQueueService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ArchiveService(Set<ArchiveDevice> archiveDevices, TempFileService fileService,
                          TelegramService telegramService, LocalisationService localisationService,
                          @Qualifier("limits") MessageService messageService, UserService userService,
                          ArchiveQueueService archiveQueueService, CommandStateService commandStateService,
                          InlineKeyboardService inlineKeyboardService) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
        this.telegramService = telegramService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.archiveQueueService = archiveQueueService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @PostConstruct
    public void init() {
        archiveQueueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("archiveTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectTask(SmartExecutorService.Job job) {
        archiveQueueService.setWaiting(job.getId());
    }

    public ArchiveTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            ArchiveQueueItem peek = archiveQueueService.poll(weight);

            if (peek != null) {
                return new ArchiveTask(peek);
            }

            return null;
        }
    }

    public SmartTempFile createArchive(int userId, List<File> files, Format archiveFormat) {
        Locale locale = userService.getLocaleOrDefault(userId);
        SmartTempFile archive = fileService.getTempFile(
                Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), archiveFormat.getExt())
        );
        ArchiveDevice archiveDevice = getCandidate(archiveFormat, locale);
        archiveDevice.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());

        return archive;
    }

    public void leave(long chatId) {
        cancelCurrentTasks(chatId);
        commandStateService.deleteState(chatId, CommandNames.RENAME_COMMAND_NAME);
    }

    public void createArchive(int userId, ArchiveState archiveState, Format format) {
        normalizeFileNames(archiveState.getFiles());

        ArchiveQueueItem item = archiveQueueService.createProcessingItem(userId, archiveState.getFiles(), format);
        startArchiveCreating(userId, item.getId());
        executor.execute(new ArchiveTask(item));
    }

    public void cancel(int userId, int jobId) {
        archiveQueueService.delete(jobId);
        executor.cancelAndComplete(jobId, true);
        ArchiveState state = commandStateService.getState(userId, CommandNames.ARCHIVE_COMMAND_NAME, true);
        commandStateService.deleteState(userId, CommandNames.RENAME_COMMAND_NAME);

        messageService.removeInlineKeyboard(userId, state.getArchiveCreatingMessageId());
    }

    private void cancelCurrentTasks(long chatId) {
        List<Integer> ids = archiveQueueService.deleteByUserId((int) chatId);
        executor.cancelAndComplete(ids, false);

        ArchiveState state = commandStateService.getState(chatId, CommandNames.RENAME_COMMAND_NAME, false);
        if (state != null) {
            messageService.removeInlineKeyboard(chatId, state.getArchiveCreatingMessageId());
        }
    }

    private void startArchiveCreating(int userId, int jobId) {
        ArchiveState state = commandStateService.getState(userId, CommandNames.ARCHIVE_COMMAND_NAME, true);
        Locale locale = new Locale(state.getLanguage());
        Message message = messageService.sendMessage(new HtmlMessage((long) userId, localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_PROCESSING, locale))
                .setReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(jobId, locale)));
        state.setArchiveCreatingMessageId(message.getMessageId());
        commandStateService.setState(userId, CommandNames.ARCHIVE_COMMAND_NAME, state);
    }

    private void normalizeFileNames(List<Any2AnyFile> any2AnyFiles) {
        Set<String> uniqueFileNames = new HashSet<>();

        for (Any2AnyFile any2AnyFile : any2AnyFiles) {
            if (!uniqueFileNames.add(any2AnyFile.getFileName())) {
                int index = 1;
                while (true) {
                    String fileName = normalizeFileName(any2AnyFile.getFileName(), index++);
                    if (uniqueFileNames.add(fileName)) {
                        any2AnyFile.setFileName(fileName);
                        break;
                    }
                }
            }
        }
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<ArchiveQueueItem> tasks = archiveQueueService.poll(jobWeight, executor.getCorePoolSize(jobWeight));
        for (ArchiveQueueItem item : tasks) {
            executor.execute(new ArchiveTask(item));
        }
    }

    private String normalizeFileName(String fileName, int index) {
        String ext = FilenameUtils.getExtension(fileName);
        if (StringUtils.isBlank(ext)) {
            return fileName + " (" + index + ")";
        }
        String name = FilenameUtils.getBaseName(fileName);

        return name + " (" + index + ")." + ext;
    }

    private ArchiveDevice getCandidate(Format format, Locale locale) {
        for (ArchiveDevice archiveDevice : archiveDevices) {
            if (archiveDevice.accept(format)) {
                return archiveDevice;
            }
        }

        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_TYPE_UNSUPPORTED, new Object[]{format}, locale));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public class ArchiveTask implements SmartExecutorService.Job {

        private int jobId;

        private List<TgFile> archiveFiles;

        private int userId;

        private Format type;

        private long totalFileSize;

        private volatile Supplier<Boolean> checker;

        private Queue<SmartTempFile> files = new LinkedBlockingQueue<>();

        private volatile SmartTempFile archive;

        private volatile boolean autoCancel;

        private ArchiveTask(ArchiveQueueItem item) {
            this.jobId = item.getId();
            this.archiveFiles = item.getFiles();
            this.totalFileSize = item.getTotalFileSize();
            this.userId = item.getUserId();
            this.type = item.getType();
        }

        @Override
        public void run() {
            String size = null;
            try {
                size = MemoryUtils.humanReadableByteCount(totalFileSize);
                LOGGER.debug("Start({}, {}, {})", userId, size, type);
                downloadFiles(archiveFiles);
                ArchiveState state = commandStateService.getState(userId, CommandNames.ARCHIVE_COMMAND_NAME, true);
                Locale locale = new Locale(state.getLanguage());

                archive = fileService.getTempFile(
                        Any2AnyFileNameUtils.getFileName(localisationService.getMessage(MessagesProperties.ARCHIVE_FILE_NAME, locale), type.getExt())
                );
                ArchiveDevice archiveDevice = getCandidate(type, locale);
                archiveDevice.zip(files.stream().map(SmartTempFile::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());
                messageService.sendDocument(new SendDocument((long) userId, archive.getFile()));

                LOGGER.debug("Finish({}, {}, {})", userId, size, type);
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
        public void cancel() {
            archiveFiles.forEach(tgFile -> telegramService.cancelDownloading(tgFile.getFileId()));
            cleanup();
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.autoCancel = !canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return totalFileSize > MemoryUtils.MB_50 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        private void cleanup() {
            if (!autoCancel) {
                archiveQueueService.delete(jobId);
                commandStateService.deleteState(userId, CommandNames.ARCHIVE_COMMAND_NAME);
            }
            executor.complete(jobId);
            files.forEach(SmartTempFile::smartDelete);
            files.clear();

            if (archive != null) {
                archive.smartDelete();
            }
        }

        private void downloadFiles(List<TgFile> tgFiles) {
            for (TgFile tgFile : tgFiles) {
                SmartTempFile file = fileService.createTempFile(tgFile.getFileName());
                telegramService.downloadFileByFileId(tgFile.getFileId(), file);
                files.add(file);
            }
        }
    }
}
