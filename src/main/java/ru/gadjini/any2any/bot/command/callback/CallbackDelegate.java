package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.command.CommandExecutor;

@Component
public class CallbackDelegate implements CallbackBotCommand {

    private CommandExecutor commandExecutor;

    @Autowired
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getName() {
        return CommandNames.CALLBACK_DELEGATE_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        String delegateCommand = requestParams.getString(Arg.CALLBACK_DELEGATE.getKey());
        CallbackBotCommand callbackCommand = commandExecutor.getCallbackCommand(delegateCommand);

        callbackCommand.processNonCommandCallback(callbackQuery, requestParams);

        return null;
    }
}
