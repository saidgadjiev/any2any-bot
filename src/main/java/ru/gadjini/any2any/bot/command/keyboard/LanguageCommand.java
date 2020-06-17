package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class LanguageCommand implements KeyboardBotCommand, NavigableBotCommand {

    private Set<String> names = new HashSet<>();

    private final LocalisationService localisationService;

    private MessageService messageService;

    private UserService userService;

    private ReplyKeyboardService replyKeyboardService;

    private CommandNavigator commandNavigator;

    @Autowired
    public LanguageCommand(LocalisationService localisationService, @Qualifier("limits") MessageService messageService,
                           UserService userService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService) {
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_NAME, locale));
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
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_LANGUAGE, locale))
                .replyKeyboard(replyKeyboardService.languageKeyboard(message.getChatId(), locale)));

        return true;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.LANGUAGE_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        text = text.toLowerCase();
        for (Locale locale: localisationService.getSupportedLocales()) {
            if (text.equals(locale.getDisplayLanguage(locale).toLowerCase())) {
                changeLocale(message, locale);
                return;
            }
        }
    }

    private void changeLocale(Message message, Locale locale) {
        userService.changeLocale(message.getFrom().getId(), locale);
        messageService.sendMessage(
                new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_LANGUAGE_SELECTED, locale))
                        .replyKeyboard(replyKeyboardService.getMainMenu(message.getChatId(), locale))
        );
        commandNavigator.silentPop(message.getChatId());
    }

}
