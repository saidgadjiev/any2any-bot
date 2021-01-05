package ru.gadjini.any2any.service.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveMessageBuilder;
import ru.gadjini.any2any.service.archive.ArchiveStep;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;

import java.util.Locale;

@Component
public class ProgressBuilder {

    private UserService userService;

    private ArchiveMessageBuilder archiveMessageBuilder;

    private LocalisationService localisationService;

    private SmartInlineKeyboardService inlineKeyboardService;

    @Autowired
    public ProgressBuilder(UserService userService, ArchiveMessageBuilder archiveMessageBuilder,
                           LocalisationService localisationService, SmartInlineKeyboardService inlineKeyboardService) {
        this.userService = userService;
        this.archiveMessageBuilder = archiveMessageBuilder;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    public Progress progressFilesDownloading(ArchiveQueueItem queueItem, int count, int current) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());
        Progress progress = new Progress();
        progress.setChatId(queueItem.getUserId());
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(archiveMessageBuilder.buildArchiveProgressMessage(queueItem, count, current, ArchiveStep.DOWNLOADING, locale));

        if (count == current) {
            String completionMessage = archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.ARCHIVE_CREATION, locale);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "10 " + seconds));
            progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }
}
