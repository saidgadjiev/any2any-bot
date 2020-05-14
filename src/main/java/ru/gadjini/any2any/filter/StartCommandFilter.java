package ru.gadjini.any2any.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
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
                              MessageService messageService, LocalisationService localisationService,
                              ReplyKeyboardService replyKeyboardService, CommandNavigator commandNavigator) {
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
            CreateOrUpdateResult createOrUpdateResult = doStart(TgMessage.getUser(update));

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

    private CreateOrUpdateResult doStart(User user) {
        CreateOrUpdateResult createOrUpdateResult = userService.createOrUpdate(user);

        if (createOrUpdateResult.isCreated()) {
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_WELCOME, new Object[]{UserUtils.userLink(user)}, createOrUpdateResult.getUser().getLocale());
            ReplyKeyboard mainMenu = replyKeyboardService.getMainMenu(createOrUpdateResult.getUser().getLocale());
            messageService.sendMessage(
                    new SendMessageContext(user.getId(), text)
                            .webPagePreview(true)
                            .replyKeyboard(mainMenu)
            );

            commandNavigator.setCurrentCommand(user.getId(), CommandNames.START_COMMAND);
        }

        return createOrUpdateResult;
    }
}
