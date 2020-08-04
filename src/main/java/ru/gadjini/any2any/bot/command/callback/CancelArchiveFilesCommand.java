package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class CancelArchiveFilesCommand implements CallbackBotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelArchiveFilesCommand(CommandStateService commandStateService, @Qualifier("messagelimits") MessageService messageService,
                                     LocalisationService localisationService, UserService userService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_ARCHIVE_FILES;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        commandStateService.deleteState(callbackQuery.getMessage().getChatId(), CommandNames.ARCHIVE_COMMAND_NAME);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFromUser().getId());
        messageService.editMessage(new EditMessageText(
                callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_DELETED, locale)
        ));
        messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                callbackQuery.getId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, locale)
        ));
    }
}
