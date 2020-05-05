package ru.gadjini.any2any.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.property.BotProperties;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandExecutor;

@Component
public class Any2AnyBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Any2AnyBot.class);

    private BotProperties botProperties;

    private MessageService messageService;

    private CommandExecutor commandExecutor;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public Any2AnyBot(BotProperties botProperties, DefaultBotOptions botOptions,
                      MessageService messageService, CommandExecutor commandExecutor,
                      LocalisationService localisationService, UserService userService) {
        super(botOptions);
        this.botProperties = botProperties;
        this.messageService = messageService;
        this.commandExecutor = commandExecutor;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            try {
                if (commandExecutor.isBotCommand(update.getMessage())) {
                    if (commandExecutor.executeBotCommand(update.getMessage())) {
                        return;
                    } else {
                        messageService.sendMessage(
                                new SendMessageContext(
                                        update.getMessage().getChatId(),
                                        localisationService.getMessage(MessagesProperties.MESSAGE_UNKNOWN_COMMAND, userService.getLocale(update.getMessage().getFrom().getId()))));
                        return;
                    }
                }
                commandExecutor.processNonCommandUpdate(update.getMessage());
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                messageService.sendErrorMessage(update.getMessage().getChatId(), userService.getLocale(update.getMessage().getFrom().getId()));
            }
        }
    }

    public String getBotUsername() {
        return botProperties.getName();
    }

    public String getBotToken() {
        return botProperties.getToken();
    }
}
