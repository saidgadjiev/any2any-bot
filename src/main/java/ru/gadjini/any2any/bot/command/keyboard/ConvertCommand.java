package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ConvertCommand implements KeyboardBotCommand, BotCommand {

    private Set<String> names = new HashSet<>();

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    @Autowired
    public ConvertCommand(UserService userService,
                          @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService) {
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        processMessage(message, (String) null);
    }

    @Override
    public String getCommandIdentifier() {
        return FileUtilsCommandNames.CONVERT_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return false;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(
                new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
        );
    }
}
