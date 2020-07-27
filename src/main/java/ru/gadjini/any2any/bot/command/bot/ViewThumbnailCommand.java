package ru.gadjini.any2any.bot.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.BotSettingsService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
public class ViewThumbnailCommand implements BotCommand {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private BotSettingsService botSettingsService;

    @Autowired
    public ViewThumbnailCommand(@Qualifier("limits") MessageService messageService, LocalisationService localisationService,
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
        if (thumbnail != null) {
            messageService.sendDocument(new SendDocument(message.getChatId(), thumbnail.getFileId()));
        } else {
            Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.VIEW_THUMBNAIL_COMMAND;
    }
}
