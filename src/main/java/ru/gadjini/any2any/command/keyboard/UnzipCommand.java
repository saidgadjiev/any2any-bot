package ru.gadjini.any2any.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class UnzipCommand implements KeyboardBotCommand, BotCommand {

    private Set<String> names = new HashSet<>();

    private LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public UnzipCommand(LocalisationService localisationService,
                        @Qualifier("messageLimits") MessageService messageService,
                        UserService userService) {
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.UNZIP_COMMAND_NAME, locale));
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
        return FileUtilsCommandNames.UNZIP_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return false;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(chatId))
                .text(localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_FILE, locale))
                .parseMode(ParseMode.HTML).build());
    }
}
