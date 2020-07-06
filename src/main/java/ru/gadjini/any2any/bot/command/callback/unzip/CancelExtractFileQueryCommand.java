package ru.gadjini.any2any.bot.command.callback.unzip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.CallbackBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.CallbackQuery;
import ru.gadjini.any2any.request.Arg;
import ru.gadjini.any2any.request.RequestParams;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.keyboard.InlineKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.unzip.UnzipMessageBuilder;
import ru.gadjini.any2any.service.unzip.UnzipService;
import ru.gadjini.any2any.service.unzip.UnzipState;

@Component
public class CancelExtractFileQueryCommand implements CallbackBotCommand {

    private UnzipService unzipService;

    private CommandStateService commandStateService;

    private LocalisationService localisationService;

    private UnzipMessageBuilder messageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    private UserService userService;

    private MessageService messageService;

    @Autowired
    public CancelExtractFileQueryCommand(UnzipService unzipService, CommandStateService commandStateService,
                                         LocalisationService localisationService, UnzipMessageBuilder messageBuilder,
                                         InlineKeyboardService inlineKeyboardService, UserService userService,
                                         @Qualifier("limits") MessageService messageService) {
        this.unzipService = unzipService;
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_EXTRACT_FILE;
    }

    @Override
    public String processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        unzipService.cancel(jobId);

        long chatId = callbackQuery.getMessage().getChatId();
        UnzipState unzipState = commandStateService.getState(chatId, CommandNames.UNZIP_COMMAND_NAME, false);
        String message = localisationService.getMessage(
                MessagesProperties.MESSAGE_ARCHIVE_FILES_LIST,
                new Object[]{messageBuilder.getFilesList(unzipState.getFiles().values())},
                userService.getLocaleOrDefault((int) chatId));
        messageService.editMessage(new EditMessageText(chatId, unzipState.getChooseFilesMessageId(), message)
                .setReplyMarkup(inlineKeyboardService.getFilesListKeyboard(unzipState.filesIds())));

        return null;
    }
}
