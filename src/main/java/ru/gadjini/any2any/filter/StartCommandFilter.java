package ru.gadjini.any2any.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.CreateOrUpdateResult;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandParser;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.utils.UserUtils;

@Component
public class StartCommandFilter extends BaseBotFilter {

    private CommandParser commandParser;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private CommandNavigator commandNavigator;

    @Autowired
    public StartCommandFilter(CommandParser commandParser, UserService userService,
                              @Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                              @Qualifier("curr") ReplyKeyboardService replyKeyboardService, CommandNavigator commandNavigator) {
        this.commandParser = commandParser;
        this.userService = userService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.commandNavigator = commandNavigator;
    }

    @Override
    public void doFilter(Update update) {
        if (isStartCommand(update)) {
            CreateOrUpdateResult createOrUpdateResult = doStart(TgMessage.from(update));

            if (createOrUpdateResult.isCreated()) {
                return;
            }
        }

        super.doFilter(update);
    }

    private boolean isStartCommand(Update update) {
        if (update.hasMessage() && update.getMessage().isCommand()) {
            String commandName = commandParser.parseBotCommandName(update.getMessage());

            return commandName.equals(CommandNames.START_COMMAND);
        }

        return false;
    }

    private CreateOrUpdateResult doStart(TgMessage message) {
        CreateOrUpdateResult createOrUpdateResult = userService.createOrUpdate(message.getUser());

        if (createOrUpdateResult.isCreated()) {
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME, new Object[]{UserUtils.userLink(message.getUser())}, createOrUpdateResult.getUser().getLocale());
            ReplyKeyboard mainMenu = replyKeyboardService.getMainMenu(message.getChatId(), createOrUpdateResult.getUser().getLocale());
            messageService.sendMessage(
                    new SendMessageContext(message.getChatId(), text)
                            .webPagePreview(true)
                            .replyKeyboard(mainMenu)
            );

            commandNavigator.setCurrentCommand(message.getChatId(), CommandNames.START_COMMAND);
        }

        return createOrUpdateResult;
    }
}
