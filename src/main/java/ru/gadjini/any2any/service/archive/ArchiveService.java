package ru.gadjini.any2any.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.any2any.service.queue.ArchiveQueueService;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class ArchiveService {

    private MessageService messageService;

    private UserService userService;

    private ArchiveQueueService archiveQueueService;

    private QueueService queueService;

    private SmartInlineKeyboardService inlineKeyboardService;

    private ArchiveMessageBuilder messageBuilder;

    @Autowired
    public ArchiveService(@Qualifier("messageLimits") MessageService messageService, UserService userService,
                          ArchiveQueueService archiveQueueService,
                          QueueService queueService, SmartInlineKeyboardService inlineKeyboardService, ArchiveMessageBuilder messageBuilder) {
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
        });
    }

    private void startArchiveCreating(ArchiveQueueItem queueItem, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        String message = messageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.WAITING, Lang.JAVA, locale);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(queueItem.getUserId()))
                .text(message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getWaitingKeyboard(queueItem.getId(), locale)).build(), callback);
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
