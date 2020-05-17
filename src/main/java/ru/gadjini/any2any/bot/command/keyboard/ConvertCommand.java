package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ConvertCommand implements KeyboardBotCommand, NavigableBotCommand {

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private ConvertMaker convertMaker;

    @Autowired
    public ConvertCommand(CommandStateService commandStateService, UserService userService,
                          @Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                          @Qualifier("currkeyboard") ReplyKeyboardService replyKeyboardService) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.CONVERT_COMMAND_NAME, locale));
        }
    }

    @Autowired
    public void setConvertMaker(ConvertMaker convertMaker) {
        this.convertMaker = convertMaker;
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        convertMaker.processNonCommandUpdate(message, text, () -> getKeyboard(message.getChatId()));
    }

    @Override
    public String getHistoryName() {
        return CommandNames.CONVERT_COMMAND_NAME;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(long chatId) {
        return replyKeyboardService.goBack(chatId, userService.getLocaleOrDefault((int) chatId));
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                        .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale))

        );

        return true;
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId);
    }

    @Override
    public boolean canLeave(long chatId) {
        Object state = commandStateService.getState(chatId, false);
        commandStateService.deleteState(chatId);

        return state == null;
    }
}
