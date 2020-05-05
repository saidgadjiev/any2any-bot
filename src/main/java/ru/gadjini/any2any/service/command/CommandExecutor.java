package ru.gadjini.any2any.service.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.common.CommandNames;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CommandExecutor {

    private final Map<String, BotCommand> botCommands = new HashMap<>();

    private CommandParser commandParser;

    @Autowired
    public CommandExecutor(Set<BotCommand> commands, CommandParser commandParser) {
        this.commandParser = commandParser;
        commands.forEach(botCommand -> botCommands.put(botCommand.getCommandIdentifier(), botCommand));
    }

    public boolean isBotCommand(Message message) {
        return message.isCommand();
    }

    public void processNonCommandUpdate(Message message) {
        botCommands.get(CommandNames.START_COMMAND).processMessage(null, message, null);
    }

    public boolean executeBotCommand(Message message) {
        CommandParser.CommandParseResult commandParseResult = commandParser.parseBotCommand(message);
        BotCommand botCommand = botCommands.get(commandParseResult.getCommandName());

        if (botCommand != null) {
            botCommand.execute(null, message.getFrom(), message.getChat(), commandParseResult.getParameters());
            return true;
        }

        return false;
    }
}
