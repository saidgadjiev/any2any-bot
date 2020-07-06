package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.archive.ArchiveService;

@Component
public class CancelArchiveQueryCommand implements CallbackBotCommand {

    private ArchiveService archiveService;

    @Autowired
    public CancelArchiveQueryCommand(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_ARCHIVE_QUERY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        archiveService.cancel(callbackQuery.getFromUser().getId(), jobId);
    }
}
