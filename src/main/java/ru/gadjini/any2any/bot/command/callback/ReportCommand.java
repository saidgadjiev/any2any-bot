package ru.gadjini.any2any.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.*;

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

        fileReportService.createReport(callbackQuery.getFrom().getId(), itemId);

        messageService.removeInlineKeyboard(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        Locale locale = userService.getLocale(callbackQuery.getFrom().getId());
        messageService.sendMessage(
                new SendMessageContext(callbackQuery.getMessage().getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_REPLY, locale))
        );

        return null;
    }
}
