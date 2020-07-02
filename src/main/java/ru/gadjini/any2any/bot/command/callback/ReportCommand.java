package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.FileReportService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class ReportCommand implements CallbackBotCommand {

    private FileReportService fileReportService;

    private MessageService messageService;

    private UserService userService;

    private LocalisationService localisationService;

    @Autowired
    public ReportCommand(FileReportService fileReportService, @Qualifier("limits") MessageService messageService,
                         UserService userService, LocalisationService localisationService) {
        this.fileReportService = fileReportService;
        this.messageService = messageService;
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Override
    public String getName() {
        return CommandNames.REPORT_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int itemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());

        fileReportService.createReport(callbackQuery.getFromUser().getId(), itemId);

        messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        Locale locale = userService.getLocaleOrDefault(callbackQuery.getFromUser().getId());
        messageService.sendMessage(
                new SendMessage(callbackQuery.getMessage().getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_REPLY, locale))
        );

        return null;
    }
}
