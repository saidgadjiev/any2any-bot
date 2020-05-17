package ru.gadjini.any2any.bot.command.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.unzip.UnzipperService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class UnzipCommand implements KeyboardBotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnzipCommand.class);

    private Set<String> names = new HashSet<>();

    private UnzipperService unzipperService;

    private LocalisationService localisationService;

    private MessageService messageService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private FormatService formatService;

    @Autowired
    public UnzipCommand(LocalisationService localisationService, UnzipperService unzipperService,
                        @Qualifier("limits") MessageService messageService, @Qualifier("currkeyboard") ReplyKeyboardService replyKeyboardService,
                        UserService userService, FormatService formatService) {
        this.localisationService = localisationService;
        this.unzipperService = unzipperService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.formatService = formatService;
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
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_FILE, locale))
                .replyKeyboard(replyKeyboardService.goBack(message.getChatId(), locale)));

        return true;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        unzipperService.unzip(message.getFrom().getId(), message.getDocument().getFileId(), checkFormat(format, message.getDocument().getMimeType(), message.getDocument().getFileName(), message.getDocument().getFileId(), locale), locale);
        messageService.sendMessage(new SendMessageContext(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZIP_PROCESSING, locale)));
    }

    @Override
    public String getHistoryName() {
        return CommandNames.UNZIP_COMMAND_NAME;
    }

    private Format checkFormat(Format format, String mimeType, String fileName, String fileId, Locale locale) {
        if (format == null) {
            LOGGER.debug("Archive format with mimeType " + mimeType + " fileName " + fileName + " fileId " + fileId + " unsupported");
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
        }
        if (format.getCategory() != FormatCategory.ARCHIVE) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SUPPORTED_ZIP_FORMATS, locale));
        }

        return format;
    }
}
