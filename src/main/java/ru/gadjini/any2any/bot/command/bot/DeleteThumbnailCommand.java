package ru.gadjini.any2any.bot.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.BotSettingsService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class DeleteThumbnailCommand implements BotCommand {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private BotSettingsService botSettingsService;

    @Autowired
    public DeleteThumbnailCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                                  UserService userService,
                                  BotSettingsService botSettingsService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.botSettingsService = botSettingsService;
    }

    @Override
    public void processMessage(Message message) {
        Any2AnyFile thumbnail = botSettingsService.getThumbnail(message.getChatId());
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        if (thumbnail != null) {
            botSettingsService.deleteThumbnail(message.getChatId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_DELETED, locale)));
        } else {
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.DEL_THUMBNAIL_COMMAND;
    }

}
