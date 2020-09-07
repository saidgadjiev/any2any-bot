package ru.gadjini.any2any.bot.command.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.service.format.ImageFormatService;
import ru.gadjini.any2any.service.keyboard.Any2AnyReplyKeyboardService;
import ru.gadjini.any2any.service.ocr.OcrService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.PhotoSize;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Comparator;
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

    private FormatService formatService;

    private ImageFormatService imageFormatService;

    @Autowired
    public OcrCommand(OcrService ocrService, @Qualifier("curr") Any2AnyReplyKeyboardService replyKeyboardService,
                      UserService userService, LocalisationService localisationService,
                      @Qualifier("messageLimits") MessageService messageService, FormatService formatService, ImageFormatService imageFormatService) {
        this.ocrService = ocrService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.formatService = formatService;
        this.imageFormatService = imageFormatService;
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
        return CommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(new HtmlMessage(chatId,
                localisationService.getMessage(MessagesProperties.MESSAGE_FILE_TO_EXTRACT, locale))
                .setReplyMarkup(replyKeyboardService.goBack(chatId, locale)));
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        ocrService.extractText(message.getFrom().getId(), getFile(message));
        messageService.sendMessage(new HtmlMessage(message.getChatId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFrom().getId()))));
    }

    private MessageMedia getFile(Message message) {
        if (message.hasDocument()) {
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            checkFormat(message.getFrom().getId(), format, message.getDocument().getFileId(), message.getDocument().getFileName(), message.getDocument().getMimeType());

            MessageMedia any2AnyFile = new MessageMedia ();
            any2AnyFile.setFileId(message.getDocument().getFileId());
            any2AnyFile.setFormat(format);

            return any2AnyFile;
        } else {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();

            MessageMedia any2AnyFile = new MessageMedia ();
            any2AnyFile.setFileId(photoSize.getFileId());
            any2AnyFile.setFormat(imageFormatService.getImageFormat(message.getChatId(), photoSize.getFileId(), photoSize.getFileSize()));

            return any2AnyFile;
        }
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
