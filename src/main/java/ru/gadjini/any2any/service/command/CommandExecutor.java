package ru.gadjini.any2any.service.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CommandExecutor {

    private Map<String, BotCommand> botCommands = new HashMap<>();

    private Collection<KeyboardBotCommand> keyboardBotCommands;

    private CommandParser commandParser;

    private CommandNavigator commandNavigator;

    @Autowired
    public CommandExecutor(CommandParser commandParser) {
        this.commandParser = commandParser;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Autowired
    public void setBotCommands(Set<BotCommand> commands) {
        commands.forEach(botCommand -> botCommands.put(botCommand.getCommandIdentifier(), botCommand));
    }

    @Autowired
    public void setKeyboardCommands(Collection<KeyboardBotCommand> keyboardCommands) {
        this.keyboardBotCommands = keyboardCommands;
    }

    public boolean isKeyboardCommand(long chatId, String text) {
        return keyboardBotCommands
                .stream()
                .anyMatch(keyboardBotCommand -> keyboardBotCommand.canHandle(chatId, text) && !keyboardBotCommand.isTextCommand());
    }

    public boolean isBotCommand(Message message) {
        return message.isCommand();
    }

    public BotCommand getBotCommand(String startCommandName) {
        return botCommands.get(startCommandName);
    }

    public void processNonCommandUpdate(Message message) {
        NavigableBotCommand navigableBotCommand = commandNavigator.getCurrentCommand(message.getChatId());

        if (navigableBotCommand != null && navigableBotCommand.accept(message)) {
            navigableBotCommand.processNonCommandUpdate(message, message.getText());
        }
    }

    public boolean executeBotCommand(Message message) {
        CommandParser.CommandParseResult commandParseResult = commandParser.parseBotCommand(message);
        BotCommand botCommand = botCommands.get(commandParseResult.getCommandName());

        if (botCommand != null) {
            botCommand.processMessage(null, message, commandParseResult.getParameters());

            if (botCommand instanceof NavigableBotCommand) {
                commandNavigator.push(message.getChatId(), (NavigableBotCommand) botCommand);
            }

            return true;
        }

        return false;
    }

    public void executeKeyBoardCommand(Message message) {
        KeyboardBotCommand botCommand = keyboardBotCommands.stream()
                .filter(keyboardBotCommand -> keyboardBotCommand.canHandle(message.getChatId(), message.getText()))
                .findFirst()
                .orElseThrow();

        boolean pushToHistory = botCommand.processMessage(message, message.getText());

        if (pushToHistory) {
            commandNavigator.push(message.getChatId(), (NavigableBotCommand) botCommand);
        }
    }

}
