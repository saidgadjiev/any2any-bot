package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.api.BotCommand;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.logging.SmartLogger;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.PhotoSize;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.OcrService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class OcrCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private static final SmartLogger LOGGER = new SmartLogger(OcrCommand.class);

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private OcrService ocrService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private FormatService formatService;

    @Autowired
    public OcrCommand(CommandStateService commandStateService, OcrService ocrService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                      UserService userService, LocalisationService localisationService,
                      @Qualifier("limits") MessageService messageService, FormatService formatService) {
        this.commandStateService = commandStateService;
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
        commandStateService.setState(chatId, getHistoryName(), locale.getLanguage());
        messageService.sendMessage(new HtmlMessage(chatId,
                localisationService.getMessage(MessagesProperties.MESSAGE_FILE_TO_EXTRACT, new Object[]{StringUtils.capitalize(locale.getDisplayLanguage(locale))}, locale))
                .setReplyMarkup(replyKeyboardService.getOcrKeyboard(chatId, locale)));
    }

    @Override
    public String getParentCommandName() {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.OCR_COMMAND_NAME;
    }

    @Override
    public boolean accept(Message message) {
        return message.hasDocument() || message.hasPhoto() || message.hasText();
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        if (message.hasText()) {
            Locale userLocale = userService.getLocaleOrDefault(message.getFromUser().getId());
            text = text.toLowerCase();
            for (Locale l : OcrService.SUPPORTED_LOCALES) {
                if (text.equals(l.getDisplayLanguage(userLocale).toLowerCase())) {
                    commandStateService.setState(message.getChatId(), getHistoryName(), l.getLanguage());
                    messageService.sendMessage(new HtmlMessage(message.getChatId(),
                            localisationService.getMessage(MessagesProperties.MESSAGE_OCR_LANGUAGE_CHANGED,
                                    new Object[]{StringUtils.capitalize(l.getDisplayLanguage(userLocale))}, userLocale)));
                    return;
                }
            }
        } else {
            Locale locale = new Locale(commandStateService.getState(message.getChatId(), getHistoryName(), true));
            ocrService.extractText(message.getFromUser().getId(), getFile(message), locale);
            messageService.sendMessage(new HtmlMessage(message.getChatId(),
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFromUser().getId()))));
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
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
            any2AnyFile.setFormat(formatService.getImageFormat(photoSize.getFileId()));

            return any2AnyFile;
        }
    }

    private void checkFormat(int userId, Format format, String fileId, String fileName, String mimeType) {
        if (format == null) {
            Locale locale = userService.getLocaleOrDefault(userId);
            LOGGER.debug("Ocr impossible", userId, mimeType, fileName, fileId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }

        if (format.getCategory() != FormatCategory.IMAGES) {
            Locale locale = userService.getLocaleOrDefault(userId);
            LOGGER.debug("Only images", userId, format.getCategory(), mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }
    }
}
