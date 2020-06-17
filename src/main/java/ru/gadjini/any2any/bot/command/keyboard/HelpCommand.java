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
import ru.gadjini.any2any.service.CommandMessageBuilder;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class HelpCommand extends BotCommand implements KeyboardBotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private Set<String> names = new HashSet<>();

    private CommandMessageBuilder commandMessageBuilder;

    @Autowired
    public HelpCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                       UserService userService, CommandMessageBuilder commandMessageBuilder) {
        super(CommandNames.HELP_COMMAND, "");
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.commandMessageBuilder = commandMessageBuilder;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.HELP_COMMAND_NAME, locale));
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        sendHelpMessage(user.getId(), userService.getLocaleOrDefault(user.getId()));
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        sendHelpMessage(message.getFrom().getId(), userService.getLocaleOrDefault(message.getFrom().getId()));

        return false;
    }

    private void sendHelpMessage(int userId, Locale locale) {
        messageService.sendMessage(
                new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_HELP,
                        new Object[]{commandMessageBuilder.getCommandsInfo(locale)},
                        locale)));
    }
}
