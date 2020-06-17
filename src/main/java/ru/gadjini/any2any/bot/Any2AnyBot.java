package ru.gadjini.any2any.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.filter.BotFilter;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.property.BotProperties;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;

@Component
public class Any2AnyBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Any2AnyBot.class);

    private BotProperties botProperties;

    private BotFilter botFilter;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public Any2AnyBot(BotProperties botProperties, DefaultBotOptions botOptions,
                      BotFilter botFilter, @Qualifier("limits") MessageService messageService, UserService userService) {
        super(botOptions);
        this.botProperties = botProperties;
        this.botFilter = botFilter;
        this.messageService = messageService;
        this.userService = userService;
    }

    public void onUpdateReceived(Update update) {
        try {
            botFilter.doFilter(update);
        } catch (UserException ex) {
            LOGGER.error(ex.getMessage(), ex);
            messageService.sendMessage(new SendMessageContext(TgMessage.getChatId(update), ex.getMessage()));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            TgMessage tgMessage = TgMessage.from(update);
            messageService.sendErrorMessage(tgMessage.getChatId(), userService.getLocaleOrDefault(tgMessage.getUser().getId()));
        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }
}
