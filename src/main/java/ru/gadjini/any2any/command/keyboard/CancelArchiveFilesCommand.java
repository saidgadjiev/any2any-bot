package ru.gadjini.any2any.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.archive.ArchiveState;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class CancelArchiveFilesCommand implements KeyboardBotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private Set<String> names = new HashSet<>();

    @Autowired
    public CancelArchiveFilesCommand(CommandStateService commandStateService, @Qualifier("messageLimits") MessageService messageService,
                                     LocalisationService localisationService, UserService userService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.CANCEL_ARCHIVE_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean processMessage(Message message, String text) {
        ArchiveState state = commandStateService.getState(message.getChatId(), FileUtilsCommandNames.ARCHIVE_COMMAND_NAME, false, ArchiveState.class);

        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (state != null) {
            commandStateService.deleteState(message.getChatId(), FileUtilsCommandNames.ARCHIVE_COMMAND_NAME);
            messageService.sendMessage(new HtmlMessage(
                    message.getChatId(),
                    state.getFiles().isEmpty()
                            ? localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_NOT_FOUND, locale)
                            : localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_DELETED, new Object[]{state.getFiles().size()}, locale)
            ));
        } else {
            messageService.sendMessage(new SendMessage(
                    message.getChatId(),
                    localisationService.getMessage(MessagesProperties.MESSAGE_ARCHIVE_FILES_NOT_FOUND, locale))
            );
        }

        return false;
    }
}
