package ru.gadjini.any2any.bot.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.service.CommandMessageBuilder;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class StartCommand implements NavigableBotCommand, BotCommand {

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private CommandMessageBuilder commandMessageBuilder;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService, @Qualifier("messagelimits") MessageService messageService,
                        LocalisationService localisationService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService, CommandMessageBuilder commandMessageBuilder) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandMessageBuilder = commandMessageBuilder;
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void processMessage(Message message) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        messageService.sendMessage(
                new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                        .setReplyMarkup(replyKeyboardService.getMainMenu(message.getChat().getId(), locale))
        );
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        messageService.sendMessage(
                new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_SECTION, new Object[]{commandMessageBuilder.getCommandsInfo(locale)}, locale))
        );
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .setReplyMarkup(replyKeyboardService.getMainMenu(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(long chatId) {
        return replyKeyboardService.getMainMenu(chatId, userService.getLocaleOrDefault((int) chatId));
    }

    @Override
    public String getMessage(long chatId) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        return localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale);
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }
}
