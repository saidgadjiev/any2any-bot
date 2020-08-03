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
import ru.gadjini.any2any.model.bot.api.object.PhotoSize;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.ocr.OcrService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class OcrCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrCommand.class);

    private Set<String> names = new HashSet<>();

    private OcrService ocrService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private FormatService formatService;

    @Autowired
    public OcrCommand(OcrService ocrService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                      UserService userService, LocalisationService localisationService,
                      @Qualifier("limits") MessageService messageService, FormatService formatService) {
        this.ocrService = ocrService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.formatService = formatService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.EXTRACT_TEXT_COMMAND_NAME, locale));
        }
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
        return CommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFromUser().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessageAsync(new HtmlMessage(chatId,
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
        ocrService.extractText(message.getFromUser().getId(), getFile(message));
        messageService.sendMessageAsync(new HtmlMessage(message.getChatId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFromUser().getId()))));
    }

    private Any2AnyFile getFile(Message message) {
        if (message.hasDocument()) {
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            checkFormat(message.getFromUser().getId(), format, message.getDocument().getFileId(), message.getDocument().getFileName(), message.getDocument().getMimeType());

            Any2AnyFile any2AnyFile = new Any2AnyFile();
            any2AnyFile.setFileId(message.getDocument().getFileId());
            any2AnyFile.setFormat(format);

            return any2AnyFile;
        } else {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();

            Any2AnyFile any2AnyFile = new Any2AnyFile();
            any2AnyFile.setFileId(photoSize.getFileId());
            any2AnyFile.setFormat(formatService.getImageFormat(message.getChatId(), photoSize.getFileId()));

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
