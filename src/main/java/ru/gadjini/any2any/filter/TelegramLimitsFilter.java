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
import ru.gadjini.any2any.model.Any2AnyFile;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.*;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@Qualifier("limits")
public class TelegramLimitsFilter extends BaseBotFilter implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLimitsFilter.class);

    public static final int TEXT_LENGTH_LIMIT = 4000;

    //1.5 GB
    private static final long LARGE_FILE_SIZE = 1610612736;

    private UserService userService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private FileService fileService;

    @Autowired
    public TelegramLimitsFilter(UserService userService, LocalisationService localisationService, FileService fileService) {
        this.userService = userService;
        this.localisationService = localisationService;
        this.fileService = fileService;
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
    public Message sendMessage(SendMessage sendMessage) {
        if (sendMessage.getText().length() < TEXT_LENGTH_LIMIT) {
            return messageService.sendMessage(sendMessage);
        } else {
            List<String> parts = new ArrayList<>();
            Splitter.fixedLength(TEXT_LENGTH_LIMIT)
                    .split(sendMessage.getText())
                    .forEach(parts::add);
            for (int i = 0; i < parts.size() - 1; ++i) {
                SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(i))
                        .setReplyToMessageId(sendMessage.getReplyToMessageId())
                        .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                        .setParseMode(sendMessage.getParseMode());
                messageService.sendMessage(msg);
            }

            SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(parts.size() - 1))
                    .setReplyToMessageId(sendMessage.getReplyToMessageId())
                    .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                    .setParseMode(sendMessage.getParseMode())
                    .setReplyMarkup(sendMessage.getReplyMarkup());
            return messageService.sendMessage(msg);
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
    public SendFileResult sendPhoto(SendPhoto sendPhoto) {
        return messageService.sendPhoto(sendPhoto);
    }

    @Override
    public SendFileResult sendVideo(SendVideo sendVideo) {
        return messageService.sendVideo(sendVideo);
    }

    @Override
    public SendFileResult sendAudio(SendAudio sendAudio) {
        return messageService.sendAudio(sendAudio);
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        messageService.sendErrorMessage(chatId, locale);
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return messageService.getMediaType(fileId);
    }

    @Override
    public void sendFile(long chatId, String fileId) {
        messageService.sendFile(chatId, fileId);
    }

    private boolean isMediaMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    private void checkInMediaSize(Message message) {
        Any2AnyFile file = fileService.getFile(message, Locale.getDefault());
        if (file.getFileSize() > LARGE_FILE_SIZE) {
            LOGGER.warn("Large in file({}, {})", message.getFromUser().getId(), MemoryUtils.humanReadableByteCount(file.getFileSize()));
            throw new UserException(localisationService.getMessage(
                    MessagesProperties.MESSAGE_TOO_LARGE_IN_FILE,
                    new Object[]{MemoryUtils.humanReadableByteCount(message.getDocument().getFileSize())},
                    userService.getLocaleOrDefault(message.getFromUser().getId())));
        } else if (file.getFileSize() > MemoryUtils.MB_100) {
            LOGGER.warn("Heavy file({}, {}, {}, {})", message.getFromUser().getId(), MemoryUtils.humanReadableByteCount(file.getFileSize()), file.getMimeType(), file.getFileName());
        }
    }

    private boolean validate(SendDocument sendDocument) {
        InputFile document = sendDocument.getDocument();
        if (StringUtils.isNotBlank(document.getFileId())) {
            return true;
        }
        File file = new File(document.getFilePath());
        if (file.length() == 0) {
            LOGGER.error("Zero file\n{}", Arrays.toString(Thread.currentThread().getStackTrace()));
            sendMessage(new SendMessage(sendDocument.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZERO_LENGTH_FILE, userService.getLocaleOrDefault((int) sendDocument.getOrigChatId())))
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        }
        if (file.length() > LARGE_FILE_SIZE) {
            LOGGER.debug("Large out file({}, {})", sendDocument.getChatId(), MemoryUtils.humanReadableByteCount(file.length()));
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{file.getName(), MemoryUtils.humanReadableByteCount(file.length())},
                    userService.getLocaleOrDefault((int) sendDocument.getOrigChatId()));

            sendMessage(new SendMessage(sendDocument.getChatId(), text)
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        } else {
            return true;
        }
    }
}
