package ru.gadjini.any2any.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.service.archive.ArchiveMessageBuilder;
import ru.gadjini.any2any.service.archive.ArchiveStep;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.progress.Lang;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueJobConfigurator;

import java.util.Locale;

@Component
public class ArchiveQueueJobConfigurator implements QueueJobConfigurator<ArchiveQueueItem> {

    private ArchiveMessageBuilder archiveMessageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ArchiveQueueJobConfigurator(ArchiveMessageBuilder messageBuilder, InlineKeyboardService inlineKeyboardService) {
        this.archiveMessageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public String getWaitingMessage(ArchiveQueueItem queueItem, Locale locale) {
        return archiveMessageBuilder.buildArchiveProcessMessage(queueItem, ArchiveStep.WAITING, Lang.JAVA, locale);
    }

    @Override
    public InlineKeyboardMarkup getWaitingKeyboard(ArchiveQueueItem queueItem, Locale locale) {
        return inlineKeyboardService.getArchiveCreatingKeyboard(queueItem.getId(), locale);
    }
}
