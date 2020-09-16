package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;
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
        return FileUtilsCommandNames.CANCEL_ARCHIVE_QUERY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        archiveService.cancel(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), callbackQuery.getId(), jobId);
    }
}
