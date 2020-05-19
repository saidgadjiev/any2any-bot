package ru.gadjini.any2any.bot.command.keyboard;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gadjini.any2any.bot.command.api.KeyboardBotCommand;
import ru.gadjini.any2any.bot.command.api.NavigableBotCommand;
import ru.gadjini.any2any.common.CommandNames;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.OcrService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.converter.api.Format;
import ru.gadjini.any2any.service.converter.api.FormatCategory;
import ru.gadjini.any2any.service.converter.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ExtractTextCommand implements KeyboardBotCommand, NavigableBotCommand {

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private OcrService ocrService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final LocalisationService localisationService;

    private MessageService messageService;

    private FormatService formatService;

    @Autowired
    public ExtractTextCommand(CommandStateService commandStateService, OcrService ocrService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
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
    public boolean processMessage(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        commandStateService.setState(message.getChatId(), locale.getLanguage());
        messageService.sendMessage(new SendMessageContext(message.getChatId(),
                localisationService.getMessage(MessagesProperties.MESSAGE_FILE_TO_EXTRACT, new Object[]{StringUtils.capitalize(locale.getDisplayLanguage(locale))}, locale))
                .replyKeyboard(replyKeyboardService.getOcrKeyboard(message.getChatId(), locale)));

        return true;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.EXTRACT_TEXT_COMMAND_NAME;
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
                    commandStateService.setState(message.getChatId(), l.getLanguage());
                    messageService.sendMessage(new SendMessageContext(message.getChatId(),
                            localisationService.getMessage(MessagesProperties.MESSAGE_OCR_LANGUAGE_CHANGED,
                                    new Object[] {StringUtils.capitalize(l.getDisplayLanguage(userLocale))}, userLocale)));
                    return;
                }
            }
        } else {
            Locale locale = new Locale(commandStateService.getState(message.getChatId(), true));
            ocrService.extractText(message.getFrom().getId(), getFile(message), locale);
            messageService.sendMessage(new SendMessageContext(message.getChatId(),
                    localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_PROCESSING, userService.getLocaleOrDefault(message.getFrom().getId()))));
        }
    }

    @Override
    public void leave(long chatId) {
        commandStateService.deleteState(chatId);
    }

    private Any2AnyFile getFile(Message message) {
        if (message.hasDocument()) {
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            checkFormat(message.getFrom().getId(), format);

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

    private void checkFormat(int userId, Format format) {
        if (format == null) {
            Locale locale = userService.getLocaleOrDefault(userId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }

        if (format.getCategory() != FormatCategory.IMAGES) {
            Locale locale = userService.getLocaleOrDefault(userId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_EXTRACTION_IMPOSSIBLE, locale));
        }
    }
}
