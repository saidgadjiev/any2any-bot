package ru.gadjini.any2any.bot.command.keyboard;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.bot.command.convert.ConvertState;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.io.SmartTempFile;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.PhotoSize;
import ru.gadjini.any2any.model.bot.api.object.Sticker;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;
import ru.gadjini.any2any.service.TempFileService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.command.CommandStateService;
import ru.gadjini.any2any.service.conversion.ConvertionService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.service.conversion.api.FormatCategory;
import ru.gadjini.any2any.service.conversion.impl.FormatService;
import ru.gadjini.any2any.service.keyboard.ReplyKeyboardService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.queue.conversion.ConversionQueueMessageBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

@Component
public class ConvertMaker {

    private static final String TAG = "convert";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertMaker.class);

    private CommandStateService commandStateService;

    private UserService userService;

    private ConvertionService conversionService;

    private ConversionQueueMessageBuilder queueMessageBuilder;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private FormatService formatService;

    private TelegramService telegramService;

    private TempFileService fileService;

    @Autowired
    public ConvertMaker(CommandStateService commandStateService, UserService userService,
                        ConvertionService conversionService, ConversionQueueMessageBuilder queueMessageBuilder, @Qualifier("limits") MessageService messageService,
                        LocalisationService localisationService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                        FormatService formatService, TelegramService telegramService, TempFileService fileService) {
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.conversionService = conversionService;
        this.queueMessageBuilder = queueMessageBuilder;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.replyKeyboardService = replyKeyboardService;
        this.formatService = formatService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public void processNonCommandUpdate(String controllerName, Message message, String text, Supplier<ReplyKeyboard> replyKeyboard) {
        Locale locale = userService.getLocaleOrDefault(message.getFromUser().getId());

        if (!commandStateService.hasState(message.getChatId(), controllerName)) {
            check(message, locale);
            ConvertState convertState = createState(message, locale);
            messageService.sendMessage(
                    new HtmlMessage(message.getChatId(), queueMessageBuilder.getChooseFormat(convertState.getWarnings(), locale))
                            .setReplyMarkup(replyKeyboardService.getFormatsKeyboard(message.getChatId(), convertState.getFormat(), locale))
            );
            convertState.deleteWarns();
            commandStateService.setState(message.getChatId(), controllerName, convertState);
        } else if (isMediaMessage(message)) {
            ConvertState convertState = commandStateService.getState(message.getChatId(), controllerName, true);
            convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_TOO_MANY_FILES, locale));
            commandStateService.setState(message.getChatId(), controllerName, convertState);
        } else if (message.hasText()) {
            ConvertState convertState = commandStateService.getState(message.getChatId(), controllerName, true);
            Format targetFormat = checkTargetFormat(message.getFromUser().getId(), convertState.getFormat(), formatService.getAssociatedFormat(text), text, locale);
            if (targetFormat == Format.GIF) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_GIF_WARN, locale));
            }
            ConversionQueueItem queueItem = conversionService.convert(message.getFromUser(), convertState, targetFormat);
            String queuedMessage = queueMessageBuilder.getQueuedMessage(queueItem, convertState.getWarnings(), new Locale(convertState.getUserLanguage()));
            messageService.sendMessage(new HtmlMessage(message.getChatId(), queuedMessage).setReplyMarkup(replyKeyboard.get()));
            commandStateService.deleteState(message.getChatId(), controllerName);
        }
    }

    private ConvertState createState(Message message, Locale locale) {
        ConvertState convertState = new ConvertState();
        convertState.setMessageId(message.getMessageId());
        convertState.setUserLanguage(locale.getLanguage());

        LOGGER.debug("Convert state({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));
        if (message.hasDocument()) {
            convertState.setFileId(message.getDocument().getFileId());
            convertState.setFileSize(message.getDocument().getFileSize());
            convertState.setFileName(message.getDocument().getFileName());
            convertState.setMimeType(message.getDocument().getMimeType());
            Format format = formatService.getFormat(message.getDocument().getFileName(), message.getDocument().getMimeType());
            convertState.setFormat(checkFormat(message.getFromUser().getId(), format, message.getDocument().getMimeType(), message.getDocument().getFileName(), locale));
            if (convertState.getFormat() == Format.HTML && isBaseUrlMissed(message.getDocument().getFileId())) {
                convertState.addWarn(localisationService.getMessage(MessagesProperties.MESSAGE_NO_BASE_URL_IN_HTML, locale));
            }
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            convertState.setFileId(photoSize.getFileId());
            convertState.setFileSize(photoSize.getFileSize());
            convertState.setFormat(Format.PHOTO);
        } else if (message.hasSticker()) {
            Sticker sticker = message.getSticker();
            convertState.setFileId(sticker.getFileId());
            convertState.setFileSize(sticker.getFileSize());
            convertState.setFormat(sticker.getAnimated() ? Format.TGS : Format.WEBP);
        } else if (message.hasText()) {
            convertState.setFileId(message.getText());
            convertState.setFileSize(message.getText().length());
            convertState.setFormat(formatService.getFormat(message.getText()));
        } else {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CONVERT_FILE, locale));
        }

        return convertState;
    }

    private boolean isBaseUrlMissed(String fileId) {
        SmartTempFile file = fileService.createTempFile(TAG, Format.HTML.getExt());

        try {
            telegramService.downloadFileByFileId(fileId, file);

            Document parse = Jsoup.parse(file.getFile(), StandardCharsets.UTF_8.name());
            Elements base = parse.head().getElementsByTag("base");

            return base == null || base.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            file.smartDelete();
        }
    }

    private void check(Message message, Locale locale) {
        if (message.hasDocument() || message.hasText() || message.hasPhoto()
                || message.hasSticker()) {
            return;
        }

        LOGGER.warn("Unsupported format({}, {})", message.getChatId(), TgMessage.getMetaTypes(message));
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private Format checkFormat(int userId, Format format, String mimeType, String fileName, Locale locale) {
        if (format == null) {
            LOGGER.warn("Format is null({}, {}, {})", userId, mimeType, fileName);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }
        if (format.getCategory() == FormatCategory.ARCHIVE) {
            LOGGER.warn("Archive unsupported({})", userId);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }

        return format;
    }

    private Format checkTargetFormat(int userId, Format format, Format target, String text, Locale locale) {
        if (target == null) {
            LOGGER.warn("Target format is null({}, {})", userId, text);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
        }
        if (Objects.equals(format, target)) {
            LOGGER.warn("Same formats({}, {})", userId, format);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_CHOOSE_ANOTHER_FORMAT, locale));
        }
        boolean result = formatService.isConvertAvailable(format, target);
        if (result) {
            return target;
        }

        LOGGER.warn("Conversion unavailable({}, {}, {})", userId, format, target);
        throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_UNSUPPORTED_FORMAT, locale));
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }
}
