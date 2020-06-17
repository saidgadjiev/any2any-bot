package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ConvertCommand extends BotCommand implements KeyboardBotCommand, NavigableBotCommand {

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
                          @Qualifier("curr") ReplyKeyboardService replyKeyboardService) {
        super(CommandNames.CONVERT_COMMAND_NAME, "");
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
        convertMaker.processNonCommandUpdate(getHistoryName(), message, text, () -> getKeyboard(message.getChatId()));
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.CONVERT_COMMAND_NAME;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
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
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        processMessage0(chat.getId(), user.getId());
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(
                new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale))
                        .replyKeyboard(replyKeyboardService.goBack(chatId, locale))

        );
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }

    @Override
    public boolean canLeave(long chatId) {
        Object state = commandStateService.getState(chatId, getHistoryName(), false);
        commandStateService.deleteState(chatId, getHistoryName());

        return state == null;
    }
}
