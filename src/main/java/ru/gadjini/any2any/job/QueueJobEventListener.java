package ru.gadjini.any2any.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.CurrentTasksCanceled;

@Component
public class QueueJobEventListener {

    private CommandStateService commandStateService;

    @Autowired
    public QueueJobEventListener(CommandStateService commandStateService) {
        this.commandStateService = commandStateService;
    }

    @EventListener
    public void currentTasksCanceled(CurrentTasksCanceled event) {
        commandStateService.deleteState(event.getUserId(), FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
    }
}
