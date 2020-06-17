package ru.gadjini.any2any.filter;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.util.Comparator;
import java.util.Locale;

@Component
@Qualifier("limits")
public class TelegramLimitsFilter extends BaseBotFilter implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLimitsFilter.class);

    private static final int TEXT_LENGTH_LIMIT = 4090;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    @Autowired
    public TelegramLimitsFilter(UserService userService, LocalisationService localisationService) {
        this.userService = userService;
        this.localisationService = localisationService;
    }

    @Autowired
    public void setMessageService(@Qualifier("message") MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage() && isMediaMessage(update.getMessage())) {
            checkInMediaSize(update.getMessage());
        }

        super.doFilter(update);
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackContext callbackContext) {
        messageService.sendAnswerCallbackQuery(callbackContext);
    }

    @Override
    public ChatMember getChatMember(String chatId, int userId) {
        return messageService.getChatMember(chatId, userId);
    }

    @Override
    public void sendMessage(SendMessageContext messageContext) {
        if (messageContext.text().length() < TEXT_LENGTH_LIMIT) {
            messageService.sendMessage(messageContext);
        } else {
            Iterable<String> split = Splitter.fixedLength(TEXT_LENGTH_LIMIT)
                    .split(messageContext.text());
            for (String text : split) {
                messageService.sendMessage(new SendMessageContext(messageContext.chatId(), text)
                        .replyKeyboard(messageContext.replyKeyboard())
                        .webPagePreview(messageContext.webPagePreview())
                        .html(messageContext.html())
                        .replyMessageId(messageContext.replyMessageId()));
            }
        }
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        messageService.removeInlineKeyboard(chatId, messageId);
    }

    @Override
    public void editMessage(EditMessageContext messageContext) {
        messageService.editMessage(messageContext);
    }

    @Override
    public void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard) {
        messageService.editReplyKeyboard(chatId, messageId, replyKeyboard);
    }

    @Override
    public void editMessageCaption(EditMessageCaptionContext context) {
        messageService.editMessageCaption(context);
    }

    @Override
    public EditMediaResult editMessageMedia(EditMediaContext editMediaContext) {
        return messageService.editMessageMedia(editMediaContext);
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        messageService.sendBotRestartedMessage(chatId, replyKeyboard, locale);
    }

    @Override
    public void sendSticker(SendFileContext sendFileContext) {
        messageService.sendSticker(sendFileContext);
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        messageService.deleteMessage(chatId, messageId);
    }

    @Override
    public SendFileResult sendDocument(SendFileContext sendDocumentContext) {
        if (validate(sendDocumentContext)) {
            return messageService.sendDocument(sendDocumentContext);
        }

        return null;
    }

    @Override
    public int sendPhoto(SendFileContext sendDocumentContext) {
        return messageService.sendPhoto(sendDocumentContext);
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        messageService.sendErrorMessage(chatId, locale);
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    private boolean isLargeFile(long size) {
        return size >= 50 * 1000 * 1000;
    }

    private void checkInMediaSize(Message message) {
        int size = 0;
        String fileId = null;
        if (message.hasDocument()) {
            Document document = message.getDocument();
            size = document.getFileSize();
            fileId = message.getDocument().getFileId();
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().stream().max(Comparator.comparing(PhotoSize::getWidth)).orElseThrow();
            size = photoSize.getFileSize();
            fileId = photoSize.getFileId();
        }
        if (size >= 20 * 1000 * 1000) {
            LOGGER.debug("Too large in file " + fileId + " size " + size);
            throw new UserException(localisationService.getMessage(
                    MessagesProperties.MESSAGE_TOO_LARGE_IN_FILE,
                    new Object[]{MemoryUtils.humanReadableByteCount(message.getDocument().getFileSize())},
                    userService.getLocaleOrDefault(message.getFrom().getId())));
        }
    }

    private boolean validate(SendFileContext sendFileContext) {
        if (sendFileContext.file().length() == 0) {
            LOGGER.debug("Empty file: " + sendFileContext.file().getAbsolutePath());
            sendMessage(new SendMessageContext(sendFileContext.chatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZERO_LENGTH_FILE, userService.getLocaleOrDefault((int) sendFileContext.chatId())))
                    .replyKeyboard(sendFileContext.replyKeyboard())
                    .replyMessageId(sendFileContext.replyMessageId()));

            return false;
        }
        boolean largeFile = isLargeFile(sendFileContext.file().length());
        if (!largeFile) {
            return true;
        } else {
            LOGGER.debug("Large out file " + sendFileContext.file().length());
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{sendFileContext.file().getName(), MemoryUtils.humanReadableByteCount(sendFileContext.file().length())},
                    userService.getLocaleOrDefault((int) sendFileContext.chatId()));

            sendMessage(new SendMessageContext(sendFileContext.chatId(), text)
                    .replyKeyboard(sendFileContext.replyKeyboard())
                    .replyMessageId(sendFileContext.replyMessageId()));

            return false;
        }
    }
}
