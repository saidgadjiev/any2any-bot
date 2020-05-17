package ru.gadjini.any2any.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.EditMessageContext;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.util.Comparator;
import java.util.Locale;

@Component
@Qualifier("limits")
public class TelegramLimitsFilter extends BaseBotFilter implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLimitsFilter.class);

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
    public ChatMember getChatMember(String chatId, int userId) {
        return messageService.getChatMember(chatId, userId);
    }

    @Override
    public void sendMessage(SendMessageContext messageContext) {
        messageService.sendMessage(messageContext);
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
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        messageService.sendBotRestartedMessage(chatId, replyKeyboard, locale);
    }

    @Override
    public void sendSticker(SendFileContext sendFileContext) {
        messageService.sendSticker(sendFileContext);
    }

    @Override
    public void sendDocument(SendFileContext sendDocumentContext) {
        boolean largeFile = isLargeFile(sendDocumentContext.file().length());
        if (!largeFile) {
            messageService.sendDocument(sendDocumentContext);
        } else {
            LOGGER.debug("Large out file " + sendDocumentContext.file().length());
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{sendDocumentContext.file().getName(), MemoryUtils.humanReadableByteCount(sendDocumentContext.file().length())},
                    userService.getLocale((int) sendDocumentContext.chatId()));

            sendMessage(new SendMessageContext(sendDocumentContext.chatId(), text)
                    .replyKeyboard(sendDocumentContext.replyKeyboard())
                    .replyMessageId(sendDocumentContext.replyMessageId()));
        }
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
                    userService.getLocale(message.getFrom().getId())));
        }
    }
}
