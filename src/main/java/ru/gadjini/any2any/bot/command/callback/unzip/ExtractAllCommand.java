package ru.gadjini.any2any.bot.command.callback.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.unzip.UnzipService;

@Component
public class ExtractAllCommand implements CallbackBotCommand {

    private UnzipService unzipService;

    @Autowired
    public ExtractAllCommand(UnzipService unzipService) {
        this.unzipService = unzipService;
    }

    @Override
    public String getName() {
        return CommandNames.EXTRACT_ALL;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int unzipJobId = requestParams.getInt(Arg.JOB_ID.getKey());

        unzipService.extractAll(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), unzipJobId, callbackQuery.getId());
    }
}
