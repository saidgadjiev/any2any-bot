package ru.gadjini.any2any.bot.command.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.unzip.UnzipService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class UnzipCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipCommand.class);

    private Set<String> names = new HashSet<>();

    private UnzipService unzipService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private FormatService formatService;

    private FileService fileService;

    @Autowired
    public UnzipCommand(LocalisationService localisationService, UnzipService unzipService,
                        @Qualifier("limits") MessageService messageService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                        UserService userService, FormatService formatService, FileService fileService) {
        this.localisationService = localisationService;
        this.unzipService = unzipService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.formatService = formatService;
        this.fileService = fileService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.UNZIP_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument();
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public void processMessage(Message message) {
        processMessage(message, null);
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.UNZIP_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFromUser().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_FILE, locale))
                .setReplyMarkup(replyKeyboardService.goBack(chatId, locale)));
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());
        Any2AnyFile file = fileService.getFile(message, locale);
        file.setFormat(checkFormat(message.getFromUser().getId(), format, message.getDocument().getMimeType(), message.getDocument().getFileName(), locale));
        unzipService.unzip(message.getFromUser().getId(), file, locale);
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.UNZIP_COMMAND_NAME;
    }

    @Override
    public void leave(long chatId) {
        unzipService.leave(chatId);
    }

    private Format checkFormat(int userId, Format format, String mimeType, String fileName, Locale locale) {
        if (format == null) {
            LOGGER.debug("Format is null({}, {}, {})", userId, mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
        }
        if (format.getCategory() != FormatCategory.ARCHIVE) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
        }

        return format;
    }
}
