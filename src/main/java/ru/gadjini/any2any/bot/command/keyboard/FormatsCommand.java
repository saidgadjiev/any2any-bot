package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
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
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatMessageBuilder;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("CPD-START")
@Component
public class FormatsCommand extends BotCommand implements KeyboardBotCommand {

    private final MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private FormatMessageBuilder formatMessageBuilder;

    private Set<String> names = new HashSet<>();

    @Autowired
    public FormatsCommand(MessageService messageService, LocalisationService localisationService, UserService userService, FormatMessageBuilder formatMessageBuilder) {
        super(CommandNames.FORMATS_COMMAND, "");
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.formatMessageBuilder = formatMessageBuilder;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.FORMATS_COMMAND_NAME, locale));
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        sendHelpMessage(user.getId(), userService.getLocale(user.getId()));
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        sendHelpMessage(message.getFrom().getId(), userService.getLocale(message.getFrom().getId()));

        return false;
    }

    private void sendHelpMessage(int userId, Locale locale) {
        String documents = formatMessageBuilder.formats(FormatCategory.DOCUMENTS);
        String images = formatMessageBuilder.formats(FormatCategory.IMAGES);

        messageService.sendMessage(
                new SendMessageContext(userId, localisationService.getMessage(MessagesProperties.MESSAGE_FORMATS, new Object[] {documents, images}, locale)));
    }
}
