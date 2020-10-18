package ru.gadjini.any2any.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class CancelArchiveFilesCommand implements CallbackBotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CancelArchiveFilesCommand(CommandStateService commandStateService, @Qualifier("messageLimits") MessageService messageService,
                                     LocalisationService localisationService, UserService userService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public String getName() {
        return FileUtilsCommandNames.CANCEL_ARCHIVE_FILES;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        commandStateService.deleteState(callbackQuery.getMessage().getChatId(), FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFrom().getId());
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
