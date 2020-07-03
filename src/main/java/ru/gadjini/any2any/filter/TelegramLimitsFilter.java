package ru.gadjini.any2any.filter;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.*;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@Qualifier("limits")
public class TelegramLimitsFilter extends BaseBotFilter implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLimitsFilter.class);

    private static final int TEXT_LENGTH_LIMIT = 4090;

    //1.5 GB
    private static final long LARGE_FILE_SIZE = 1610612736;

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
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        messageService.sendAnswerCallbackQuery(answerCallbackQuery);
    }

    @Override
    public ChatMember getChatMember(String chatId, int userId) {
        return messageService.getChatMember(chatId, userId);
    }

    @Override
    public void sendMessage(SendMessage sendMessage) {
        if (sendMessage.getText().length() < TEXT_LENGTH_LIMIT) {
            messageService.sendMessage(sendMessage);
        } else {
            List<String> parts = new ArrayList<>();
            Splitter.fixedLength(TEXT_LENGTH_LIMIT)
                    .split(sendMessage.getText())
                    .forEach(parts::add);
            for (int i = 0; i < parts.size(); ++i) {
                SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(i))
                        .setReplyToMessageId(sendMessage.getReplyToMessageId())
                        .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                        .setParseMode(sendMessage.getParseMode());
                if (i + 1 == parts.size()) {
                    messageService.sendMessage(msg);
                } else {
                    msg.setReplyMarkup(sendMessage.getReplyMarkup());
                    messageService.sendMessage(sendMessage);
                }
            }
        }
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        messageService.removeInlineKeyboard(chatId, messageId);
    }

    @Override
    public void editMessage(EditMessageText messageContext) {
        messageService.editMessage(messageContext);
    }

    @Override
    public void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard) {
        messageService.editReplyKeyboard(chatId, messageId, replyKeyboard);
    }

    @Override
    public void editMessageCaption(EditMessageCaption context) {
        messageService.editMessageCaption(context);
    }

    @Override
    public EditMediaResult editMessageMedia(EditMessageMedia editMediaContext) {
        return messageService.editMessageMedia(editMediaContext);
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        messageService.sendBotRestartedMessage(chatId, replyKeyboard, locale);
    }

    @Override
    public void sendSticker(SendSticker sendSticker) {
        messageService.sendSticker(sendSticker);
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        messageService.deleteMessage(chatId, messageId);
    }

    @Override
    public SendFileResult sendDocument(SendDocument sendDocument) {
        if (validate(sendDocument)) {
            return messageService.sendDocument(sendDocument);
        }

        return null;
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        messageService.sendErrorMessage(chatId, locale);
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    private boolean isLargeFile(long size) {
        return size > LARGE_FILE_SIZE;
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
        if (size > LARGE_FILE_SIZE) {
            LOGGER.debug("Too large in file({}, {}, {})", fileId, size, message.getFromUser().getId());
            throw new UserException(localisationService.getMessage(
                    MessagesProperties.MESSAGE_TOO_LARGE_IN_FILE,
                    new Object[]{MemoryUtils.humanReadableByteCount(message.getDocument().getFileSize())},
                    userService.getLocaleOrDefault(message.getFromUser().getId())));
        }
    }

    private boolean validate(SendDocument sendDocument) {
        InputFile document = sendDocument.getDocument();
        if (StringUtils.isNotBlank(document.getFileId())) {
            return true;
        }
        File file = new File(document.getFilePath());
        if (file.length() == 0) {
            LOGGER.debug("Empty file({}, {})", document.getFilePath(), sendDocument);
            sendMessage(new SendMessage(sendDocument.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZERO_LENGTH_FILE, userService.getLocaleOrDefault(sendDocument.getUserId())))
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        }
        boolean largeFile = isLargeFile(file.length());
        if (!largeFile) {
            return true;
        } else {
            LOGGER.debug("Too large out file({}, {})", file.length(), sendDocument.getUserId());
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{file.getName(), MemoryUtils.humanReadableByteCount(file.length())},
                    userService.getLocaleOrDefault(sendDocument.getUserId()));

            sendMessage(new SendMessage(sendDocument.getChatId(), text)
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        }
    }
}
