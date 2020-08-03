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
import java.util.function.Consumer;

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
    public void sendMessageAsync(SendMessage sendMessage) {
        messageService.sendMessageAsync(sendMessage);
    }

    @Override
    public void sendMessageAsync(SendMessage sendMessage, Consumer<Message> callback) {
        if (sendMessage.getText().length() < TEXT_LENGTH_LIMIT) {
            messageService.sendMessageAsync(sendMessage, callback);
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
                messageService.sendMessageAsync(msg);
            }

            SendMessage msg = new SendMessage(sendMessage.getChatId(), parts.get(parts.size() - 1))
                    .setReplyToMessageId(sendMessage.getReplyToMessageId())
                    .setDisableWebPagePreview(sendMessage.getDisableWebPagePreview())
                    .setParseMode(sendMessage.getParseMode())
                    .setReplyMarkup(sendMessage.getReplyMarkup());
            messageService.sendMessageAsync(msg, callback);
        }
    }

    @Override
    public void removeInlineKeyboardAsync(long chatId, int messageId) {
        messageService.removeInlineKeyboardAsync(chatId, messageId);
    }

    @Override
    public void editMessageAsync(EditMessageText messageContext) {
        messageService.editMessageAsync(messageContext);
    }

    @Override
    public void editMessageCaptionAsync(EditMessageCaption context) {
        messageService.editMessageCaptionAsync(context);
    }

    @Override
    public void editMessageMediaAsync(EditMessageMedia editMediaContext, Consumer<EditMediaResult> consumer) {
        messageService.editMessageMediaAsync(editMediaContext, consumer);
    }

    @Override
    public void editMessageMediaAsync(EditMessageMedia editMediaContext) {
        messageService.editMessageMediaAsync(editMediaContext);
    }

    @Override
    public void sendBotRestartedMessageAsync(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        messageService.sendBotRestartedMessageAsync(chatId, replyKeyboard, locale);
    }

    @Override
    public void sendStickerAsync(SendSticker sendSticker) {
        messageService.sendStickerAsync(sendSticker);
    }

    @Override
    public void deleteMessageAsync(long chatId, int messageId) {
        messageService.deleteMessageAsync(chatId, messageId);
    }

    @Override
    public void sendDocumentAsync(SendDocument sendDocument) {
        if (validate(sendDocument)) {
            messageService.sendDocumentAsync(sendDocument);
        }
    }

    @Override
    public void sendDocumentAsync(SendDocument sendDocument, Consumer<SendFileResult> callback) {
        if (validate(sendDocument)) {
            messageService.sendDocumentAsync(sendDocument, callback);
        }
    }

    @Override
    public void sendPhotoAsync(SendPhoto sendPhoto) {
        messageService.sendPhotoAsync(sendPhoto);
    }

    @Override
    public void sendPhotoAsync(SendPhoto sendPhoto, Consumer<SendFileResult> callback) {
        messageService.sendPhotoAsync(sendPhoto, callback);
    }

    @Override
    public void sendVideoAsync(SendVideo sendVideo) {
        messageService.sendVideoAsync(sendVideo);
    }

    @Override
    public void sendAudioAsync(SendAudio sendAudio) {
        messageService.sendAudioAsync(sendAudio);
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
    public void sendFileAsync(long chatId, String fileId) {
        messageService.sendFileAsync(chatId, fileId);
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
            sendMessageAsync(new SendMessage(sendDocument.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_ZERO_LENGTH_FILE, userService.getLocaleOrDefault((int) sendDocument.getOrigChatId())))
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        }
        if (file.length() > LARGE_FILE_SIZE) {
            LOGGER.debug("Large out file({}, {})", sendDocument.getChatId(), MemoryUtils.humanReadableByteCount(file.length()));
            String text = localisationService.getMessage(MessagesProperties.MESSAGE_TOO_LARGE_OUT_FILE,
                    new Object[]{file.getName(), MemoryUtils.humanReadableByteCount(file.length())},
                    userService.getLocaleOrDefault((int) sendDocument.getOrigChatId()));

            sendMessageAsync(new SendMessage(sendDocument.getChatId(), text)
                    .setReplyMarkup(sendDocument.getReplyMarkup())
                    .setReplyToMessageId(sendDocument.getReplyToMessageId()));

            return false;
        } else {
            return true;
        }
    }
}
