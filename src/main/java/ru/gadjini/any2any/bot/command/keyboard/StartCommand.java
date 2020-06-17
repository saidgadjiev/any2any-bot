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
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.Locale;

@Component
public class StartCommand extends BotCommand implements NavigableBotCommand {

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private ConvertMaker convertMaker;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService, @Qualifier("limits") MessageService messageService,
                        LocalisationService localisationService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService) {
        super(CommandNames.START_COMMAND, "");
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
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
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        Locale locale = userService.getLocaleOrDefault(user.getId());
        messageService.sendMessage(
                new SendMessageContext(chat.getId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                        .replyKeyboard(replyKeyboardService.getMainMenu(chat.getId(), locale))
        );
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        convertMaker.processNonCommandUpdate(getHistoryName(), message, text, () -> getKeyboard(message.getChatId()));
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .replyKeyboard(replyKeyboardService.getMainMenu(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(long chatId) {
        return replyKeyboardService.getMainMenu(chatId, userService.getLocaleOrDefault((int) chatId));
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }
}
