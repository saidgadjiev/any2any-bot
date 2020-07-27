package ru.gadjini.any2any.bot.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.any2any.service.BotSettingsService;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.command.navigator.CommandNavigator;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;

@Component
public class SetThumbnailCommand implements BotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetThumbnailCommand.class);

    private CommandStateService commandStateService;

    private CommandNavigator commandNavigator;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ReplyKeyboardService replyKeyboardService;

    private BotSettingsService botSettingsService;

    private FileService fileService;

    @Autowired
    public SetThumbnailCommand(CommandStateService commandStateService,
                               @Qualifier("limits") MessageService messageService, LocalisationService localisationService,
                               UserService userService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                               BotSettingsService botSettingsService, FileService fileService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.botSettingsService = botSettingsService;
        this.fileService = fileService;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    @Override
    public void processMessage(Message message) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_SEND_THUMB, locale))
                .setReplyMarkup(replyKeyboardService.cancel(message.getChatId(), locale)));
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        Any2AnyFile any2AnyFile = fileService.getFile(message, locale);

        if (any2AnyFile != null) {
            validate(message.getFromUser().getId(), any2AnyFile, locale);
            botSettingsService.setThumbnail(message.getChatId(), any2AnyFile);

            ReplyKeyboardMarkup replyKeyboardMarkup = commandNavigator.silentPop(message.getChatId());
            messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_ADDED, locale))
                    .setReplyMarkup(replyKeyboardMarkup));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.SET_THUMBNAIL_COMMAND;
    }

    @Override
    public String getParentCommandName(long chatId) {
        String commandName = commandStateService.getState(chatId, CommandNames.SET_THUMBNAIL_COMMAND, false);

        return StringUtils.isBlank(commandName) ? CommandNames.START_COMMAND : commandName;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.SET_THUMBNAIL_COMMAND;
    }

    @Override
    public void setPrevCommand(long chatId, String prevCommand) {
        commandStateService.setState(chatId, CommandNames.SET_THUMBNAIL_COMMAND, prevCommand);
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, CommandNames.SET_THUMBNAIL_COMMAND);
    }

    private void validate(int userId, Any2AnyFile any2AnyFile, Locale locale) {
        if (!Objects.equals(any2AnyFile.getFormat().getCategory(), FormatCategory.IMAGES)) {
            LOGGER.debug("Non image thumb({}, {})", userId, any2AnyFile.getFormat());
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_INVALID_FILE, locale));
        }
    }
}
