package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.queue.ArchiveQueueService;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class ArchiveService {

    private static final String TAG = "archive";

    private FileManager fileManager;

    private MessageService messageService;

    private UserService userService;

    private ArchiveQueueService archiveQueueService;

    private QueueService queueService;

    private InlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder messageBuilder;

    @Autowired
    public ArchiveService(FileManager fileManager,
                          @Qualifier("messageLimits") MessageService messageService, UserService userService,
                          ArchiveQueueService archiveQueueService,
                          QueueService queueService, InlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder messageBuilder) {
        this.fileManager = fileManager;
        this.messageService = messageService;
        this.userService = userService;
        this.archiveQueueService = archiveQueueService;
        this.queueService = queueService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.messageBuilder = messageBuilder;
    }

    public void createArchive(int userId, ArchiveState archiveState, Format format) {
        normalizeFileNames(archiveState.getFiles());

        ArchiveQueueItem item = archiveQueueService.createItem(userId, archiveState.getFiles(), format);
        startArchiveCreating(item, message -> {
            queueService.setProgressMessageId(item.getId(), message.getMessageId());
            item.setProgressMessageId(message.getMessageId());
            fileManager.setInputFilePending(userId, null, null, item.getTotalFileSize(), TAG);
        });
    }

    private void startArchiveCreating(ArchiveQueueItem queueItem, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        String message = messageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.WAITING, queueItem.getTotalFileSize(), Lang.JAVA, locale);
        messageService.sendMessage(new HtmlMessage((long) queueItem.getUserId(), message)
                .setReplyMarkup(inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale)), callback);
    }

    private void normalizeFileNames(List<MessageMedia> any2AnyFiles) {
        Set<String> uniqueFileNames = new HashSet<>();

        for (MessageMedia any2AnyFile : any2AnyFiles) {
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

    private String normalizeFileName(String fileName, int index) {
        String ext = FilenameUtils.getExtension(fileName);
        if (StringUtils.isBlank(ext)) {
            return fileName + " (" + index + ")";
        }
        String name = FilenameUtils.getBaseName(fileName);

        return name + " (" + index + ")." + ext;
    }
}
