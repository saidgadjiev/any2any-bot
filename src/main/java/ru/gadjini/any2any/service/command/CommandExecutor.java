package ru.gadjini.any2any.service.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.bot.command.api.*;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.command.navigator.CallbackCommandNavigator;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    private Map<String, BotCommand> botCommands = new HashMap<>();

    private Collection<KeyboardBotCommand> keyboardBotCommands;

    private final Map<String, CallbackBotCommand> callbackBotCommands = new HashMap<>();

    private CommandParser commandParser;

    private CommandNavigator commandNavigator;

    private CallbackCommandNavigator callbackCommandNavigator;

    private MessageService messageService;

    @Autowired
    public CommandExecutor(CommandParser commandParser, @Qualifier("limits") MessageService messageService) {
        this.commandParser = commandParser;
        this.messageService = messageService;
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

    @Autowired
    public void setCallbackBotCommands(Collection<CallbackBotCommand> commands) {
        commands.forEach(callbackBotCommand -> callbackBotCommands.put(callbackBotCommand.getName(), callbackBotCommand));
    }

    @Autowired
    public void setCallbackCommandNavigator(CallbackCommandNavigator callbackCommandNavigator) {
        this.callbackCommandNavigator = callbackCommandNavigator;
    }

    public CallbackBotCommand getCallbackCommand(String commandName) {
        return callbackBotCommands.get(commandName);
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

    public void cancelCommand(long chatId, String queryId) {
        NavigableBotCommand navigableBotCommand = commandNavigator.getCurrentCommand(chatId);

        if (navigableBotCommand != null) {
            navigableBotCommand.cancel(chatId, queryId);
        }
    }

    public void processNonCommandUpdate(Message message, String text) {
        NavigableBotCommand navigableBotCommand = commandNavigator.getCurrentCommand(message.getChatId());

        if (navigableBotCommand != null && navigableBotCommand.accept(message)) {
            navigableBotCommand.processNonCommandUpdate(message, text);
        }
    }

    public boolean executeBotCommand(Message message) {
        CommandParser.CommandParseResult commandParseResult = commandParser.parseBotCommand(message);
        BotCommand botCommand = botCommands.get(commandParseResult.getCommandName());

        if (botCommand != null) {
            LOGGER.debug("Bot command " + botCommand.getClass().getSimpleName() + "(" + message.getFromUser().getId() + ")");
            botCommand.processMessage(message);

            if (botCommand instanceof NavigableBotCommand) {
                commandNavigator.push(message.getChatId(), (NavigableBotCommand) botCommand);
            }

            return true;
        }

        return false;
    }

    public void executeKeyBoardCommand(Message message, String text) {
        KeyboardBotCommand botCommand = keyboardBotCommands.stream()
                .filter(keyboardBotCommand -> keyboardBotCommand.canHandle(message.getChatId(), text))
                .findFirst()
                .orElseThrow();

        LOGGER.debug("Keyboard command " + botCommand.getClass().getSimpleName() + "(" + message.getFromUser().getId() + ")");
        boolean pushToHistory = botCommand.processMessage(message, message.getText());

        if (pushToHistory) {
            commandNavigator.push(message.getChatId(), (NavigableBotCommand) botCommand);
        }
    }

    public void executeCallbackCommand(CallbackQuery callbackQuery) {
        CommandParser.CommandParseResult parseResult = commandParser.parseCallbackCommand(callbackQuery);
        CallbackBotCommand botCommand = callbackBotCommands.get(parseResult.getCommandName());

        LOGGER.debug("Callback command " + botCommand.getClass().getSimpleName() + "(" + callbackQuery.getFromUser().getId() + ")");
        try {
            if (botCommand instanceof NavigableCallbackBotCommand) {
                callbackCommandNavigator.push(callbackQuery.getMessage().getChatId(), (NavigableCallbackBotCommand) botCommand);
            }
            botCommand.processMessage(callbackQuery, parseResult.getRequestParams());
        } catch (Exception ex) {
            if (botCommand instanceof NavigableCallbackBotCommand) {
                callbackCommandNavigator.silentPop(callbackQuery.getMessage().getChatId());
            }
            throw ex;
        }
    }
}
