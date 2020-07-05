package ru.gadjini.any2any.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.keyboard.rename.RenameState;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.rename.RenameQueueService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private TelegramService telegramService;

    private TempFileService fileService;

    private FormatService formatService;

    private MessageService messageService;

    private RenameQueueService renameQueueService;

    private UserService userService;

    private ThreadPoolExecutor executor;

    @Autowired
    public RenameService(TelegramService telegramService, TempFileService fileService, FormatService formatService,
                         @Qualifier("limits") MessageService messageService, RenameQueueService renameQueueService, UserService userService) {
        this.telegramService = telegramService;
        this.fileService = fileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.renameQueueService = renameQueueService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        renameQueueService.resetProcessing();
    }

    @Autowired
    public void setExecutor(@Qualifier("renameTaskExecutor") ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public void rejectRenameTask(RenameTask renameTask) {
        renameQueueService.setWaiting(renameTask.jobId);
    }

    public RenameTask getTask() {
        synchronized (this) {
            RenameQueueItem peek = renameQueueService.peek();

            if (peek != null) {
                return new RenameTask(peek.getId(), peek.getUserId(), peek.getFile().getFileName(), peek.getNewFileName(), peek.getFile().getMimeType(),
                        peek.getFile().getFileId(), peek.getReplyToMessageId(), userService.getLocaleOrDefault(peek.getUserId()));
            }
            return null;
        }
    }

    public void rename(int userId, RenameState renameState, String newFileName, Locale locale) {
        int jobId = renameQueueService.createProcessingItem(userId, renameState, newFileName);
        executor.execute(new RenameTask(jobId, userId, renameState.getFile().getFileName(), newFileName,
                renameState.getFile().getMimeType(), renameState.getFile().getFileId(), renameState.getReplyMessageId(),
                locale));
    }

    private SmartTempFile createNewFile(String fileName, String ext) {
        if (StringUtils.isNotBlank(ext)) {
            String withExt = FilenameUtils.getExtension(fileName);

            if (StringUtils.isBlank(withExt)) {
                return fileService.createTempFile(fileName + "." + ext);
            } else {
                return fileService.createTempFile(fileName);
            }
        }

        return fileService.createTempFile(fileName);
    }

    private void sendMessage(long chatId, int replyMessageId, File renamed) {
        try {
            messageService.sendDocument(new SendDocument(chatId, renamed).setReplyToMessageId(replyMessageId));
        } finally {
            FileUtils.deleteQuietly(renamed);
        }
    }

    public final class RenameTask implements Runnable {

        private int jobId;
        private final int userId;
        private final String fileName;
        private final String newFileName;
        private final String mimeType;
        private final String fileId;
        private final int replyToMessageId;
        private final Locale locale;

        private RenameTask(int jobId,
                           int userId,
                           String fileName,
                           String newFileName,
                           String mimeType,
                           String fileId,
                           int replyToMessageId,
                           Locale locale) {
            this.jobId = jobId;
            this.userId = userId;
            this.fileName = fileName;
            this.newFileName = newFileName;
            this.mimeType = mimeType;
            this.fileId = fileId;
            this.replyToMessageId = replyToMessageId;
            this.locale = locale;
        }

        @Override
        public void run() {
            String ext = formatService.getExt(fileName, mimeType);
            SmartTempFile file = createNewFile(newFileName, ext);
            telegramService.downloadFileByFileId(fileId, file.getFile());
            try {
                sendMessage(userId, replyToMessageId, file.getFile());
                LOGGER.debug("Rename success for " + userId + " new file name " + newFileName);
            } catch (Exception ex) {
                messageService.sendErrorMessage(userId, locale);
                throw ex;
            } finally {
                renameQueueService.delete(jobId);
                file.smartDelete();
            }
        }
    }
}
