package ru.gadjini.any2any.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.Any2AnyCommandMessageBuilder;
import ru.gadjini.any2any.service.keyboard.Any2AnyReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class StartCommand implements NavigableBotCommand, BotCommand {

    private CommandStateService commandStateService;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private Any2AnyReplyKeyboardService replyKeyboardService;

    private Any2AnyCommandMessageBuilder commandMessageBuilder;

    @Autowired
    public StartCommand(CommandStateService commandStateService, UserService userService, @Qualifier("messageLimits") MessageService messageService,
                        LocalisationService localisationService, @Qualifier("curr") Any2AnyReplyKeyboardService replyKeyboardService, Any2AnyCommandMessageBuilder commandMessageBuilder) {
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
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                        .setReplyMarkup(replyKeyboardService.getMainMenu(message.getChat().getId(), locale))
        );
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(
                new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_SECTION, new Object[]{commandMessageBuilder.getCommandsInfo(locale)}, locale))
        );
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public void restore(TgMessage message) {
        commandStateService.deleteState(message.getChatId(), getHistoryName());
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_MAIN_MENU, locale))
                .setReplyMarkup(replyKeyboardService.getMainMenu(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboard getKeyboard(long chatId) {
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
