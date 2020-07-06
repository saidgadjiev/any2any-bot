package ru.gadjini.any2any.bot.command.callback.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.unzip.UnzipService;

@Component
public class ExtractFileCommand implements CallbackBotCommand {

    private UnzipService unzipService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private CommandStateService commandStateService;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ExtractFileCommand(UnzipService unzipService, @Qualifier("limits") MessageService messageService,
                              LocalisationService localisationService, UserService userService,
                              CommandStateService commandStateService, InlineKeyboardService inlineKeyboardService) {
        this.unzipService = unzipService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.commandStateService = commandStateService;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public String getName() {
        return CommandNames.EXTRACT_FILE_COMMAND_NAME;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int id = requestParams.getInt(Arg.EXTRACT_FILE_ID.getKey());

        unzipService.extractFile(callbackQuery.getFromUser().getId(), id);

        return null;
    }
}
