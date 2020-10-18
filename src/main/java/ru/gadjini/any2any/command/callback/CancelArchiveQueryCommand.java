package ru.gadjini.any2any.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.job.ArchiverJob;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

@Component
public class CancelArchiveQueryCommand implements CallbackBotCommand {

    private ArchiverJob archiverJob;

    @Autowired
    public CancelArchiveQueryCommand(ArchiverJob archiverJob) {
        this.archiverJob = archiverJob;
    }

    @Override
    public String getName() {
        return FileUtilsCommandNames.CANCEL_ARCHIVE_QUERY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        archiverJob.cancel(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), callbackQuery.getId(), jobId);
    }
}
