package ru.gadjini.any2any.service.command;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;

@Service
public class CommandParser {

    public static final String COMMAND_ARG_SEPARATOR = "=";

    public static final String COMMAND_NAME_SEPARATOR = ":";

    public String parseCallbackCommandName(CallbackQuery callbackQuery) {
        String text = callbackQuery.getData();
        String[] commandSplit = text.split(COMMAND_NAME_SEPARATOR);

        return commandSplit[0];
    }

    public CommandParseResult parseBotCommand(Message message) {
        String text = message.getText().trim();
        String[] commandSplit = text.split(COMMAND_ARG_SEPARATOR);
        String[] parameters = Arrays.copyOfRange(commandSplit, 1, commandSplit.length);

        return new CommandParseResult(commandSplit[0].substring(1), parameters);
    }

    public String parseBotCommandName(Message message) {
        String text = message.getText().trim();
        String[] commandSplit = text.split(COMMAND_ARG_SEPARATOR);

        return commandSplit[0].substring(1);
    }

    public static class CommandParseResult {

        private String commandName;

        private String[] parameters;

        public CommandParseResult(String commandName, String[] parameters) {
            this.commandName = commandName;
            this.parameters = parameters;
        }

        public String getCommandName() {
            return commandName;
        }

        public String[] getParameters() {
            return parameters;
        }
    }
}
