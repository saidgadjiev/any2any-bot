package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("CPD-START")
@Component
public class FormatsCommand extends BotCommand implements KeyboardBotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private Set<String> names = new HashSet<>();

    @Autowired
    public FormatsCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService, UserService userService) {
        super(CommandNames.FORMATS_COMMAND, "");
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_NAME, locale));
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        processMessage0(user.getId(), userService.getLocaleOrDefault(user.getId()));
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getFrom().getId(), userService.getLocaleOrDefault(message.getFrom().getId()));

        return false;
    }

    private void processMessage0(int userId, Locale locale) {
        messageService.sendMessage(
                new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_FORMATS, locale)));
    }
}
