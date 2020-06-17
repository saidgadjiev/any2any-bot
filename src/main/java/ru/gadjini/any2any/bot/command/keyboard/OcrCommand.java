package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.OcrService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class OcrCommand extends BotCommand implements KeyboardBotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrCommand.class);

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
        super(CommandNames.OCR_COMMAND_NAME, "");
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
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        processMessage0(chat.getId(), user.getId());
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        commandStateService.setState(chatId, getHistoryName(), locale.getLanguage());
        messageService.sendMessage(new SendMessageContext(chatId,
                localisationService.getMessage(MessagesProperties.MESSAGE_FILE_TO_EXTRACT, new Object[]{StringUtils.capitalize(locale.getDisplayLanguage(locale))}, locale))
                .replyKeyboard(replyKeyboardService.getOcrKeyboard(chatId, locale)));
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
            Locale userLocale = userService.getLocaleOrDefault(message.getFrom().getId());
            text = text.toLowerCase();
            for (Locale l : OcrService.SUPPORTED_LOCALES) {
                if (text.equals(l.getDisplayLanguage(userLocale).toLowerCase())) {
                    commandStateService.setState(message.getChatId(), getHistoryName(), l.getLanguage());
                    messageService.sendMessage(new SendMessageContext(message.getChatId(),
                            localisationService.getMessage(MessagesProperties.MESSAGE_OCR_LANGUAGE_CHANGED,
                                    new Object[]{StringUtils.capitalize(l.getDisplayLanguage(userLocale))}, userLocale)));
                    return;
                }
            }
        } else {
            Locale locale = new Locale(commandStateService.getState(message.getChatId(), getHistoryName(), true));
            ocrService.extractText(message.getFrom().getId(), getFile(message), locale);
            messageService.sendMessage(new SendMessageContext(message.getChatId(),
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFrom().getId()))));
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId, getHistoryName());
    }

    private Any2AnyFile getFile(Message message) {
        if (message.hasDocument()) {
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            checkFormat(message.getFrom().getId(), format, message.getDocument().getFileId(), message.getDocument().getFileName(), message.getDocument().getMimeType());

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
            LOGGER.debug("Ocr impossible for user " + userId + " file id " + fileId + " fileName " + fileName + " mimeType " + mimeType);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }

        if (format.getCategory() != FormatCategory.IMAGES) {
            Locale locale = userService.getLocaleOrDefault(userId);
            LOGGER.debug("Ocr impossible for category " + format.getCategory() + " for user " + userId + " file id " + fileId + " fileName " + fileName + " mimeType " + mimeType);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }
    }
}
