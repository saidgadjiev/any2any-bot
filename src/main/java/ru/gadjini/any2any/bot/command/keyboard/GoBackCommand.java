package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class GoBackCommand implements KeyboardBotCommand, BotCommand {

    private CommandNavigator commandNavigator;

    private Set<String> names = new HashSet<>();

    @Autowired
    public GoBackCommand(LocalisationService localisationService) {
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale));
        }
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        commandNavigator.pop(TgMessage.from(message));

        return false;
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.GO_BACK;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        commandNavigator.pop(TgMessage.from(message));
    }
}
