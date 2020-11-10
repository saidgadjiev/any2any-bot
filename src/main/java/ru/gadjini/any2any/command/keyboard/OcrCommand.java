package ru.gadjini.any2any.command.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.any2any.common.FileUtilsCommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.keyboard.Any2AnyReplyKeyboardService;
import ru.gadjini.any2any.service.ocr.OcrService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.common.CommandNames;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class OcrCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrCommand.class);

    private Set<String> names = new HashSet<>();

    private OcrService ocrService;

    private Any2AnyReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private MessageMediaService messageMediaService;

    @Autowired
    public OcrCommand(OcrService ocrService, @Qualifier("curr") Any2AnyReplyKeyboardService replyKeyboardService,
                      UserService userService, LocalisationService localisationService,
                      @Qualifier("messageLimits") MessageService messageService, MessageMediaService messageMediaService) {
        this.ocrService = ocrService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.messageMediaService = messageMediaService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.EXTRACT_TEXT_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public void processMessage(Message message, String[] params) {
        processMessage(message, (String) null);
    }

    @Override
    public String getCommandIdentifier() {
        return FileUtilsCommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(chatId))
                .text(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_TO_EXTRACT, locale))
                .parseMode(ParseMode.HTML)
                .replyMarkup(replyKeyboardService.goBack(chatId, locale)).build());
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND_NAME;
    }

    @Override
    public String getHistoryName() {
        return FileUtilsCommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean acceptNonCommandMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        ocrService.extractText(message.getFrom().getId(), getFile(message));
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                .text(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFrom().getId())))
                .parseMode(ParseMode.HTML).build());
    }

    private MessageMedia getFile(Message message) {
        MessageMedia media = messageMediaService.getMedia(message, Locale.getDefault());

        if (message.hasDocument()) {
            checkFormat(message.getFrom().getId(), media.getFormat(), media.getFileId(), media.getFileName(), media.getMimeType());
        }

        return media;
    }

    private void checkFormat(int userId, Format format, String fileId, String fileName, String mimeType) {
        if (format == null) {
            Locale locale = userService.getLocaleOrDefault(userId);
            LOGGER.warn("Ocr impossible({}, {}, {}, {})", userId, mimeType, fileName, fileId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }

        if (format.getCategory() != FormatCategory.IMAGES) {
            Locale locale = userService.getLocaleOrDefault(userId);
            LOGGER.warn("Only images({}, {}, {}, {})", userId, format.getCategory(), mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }
    }
}
