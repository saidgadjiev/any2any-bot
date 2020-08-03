package ru.gadjini.any2any.service.message;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.botapi.TelegramApiException;
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.job.TgMethodExecutor;
import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.ParseMode;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private LocalisationService localisationService;

    private FileService fileService;

    private TelegramService telegramService;

    private TgMethodExecutor messageSenderJob;

    @Autowired
    public MessageServiceImpl(LocalisationService localisationService, FileService fileService,
                              TelegramService telegramService, TgMethodExecutor messageSenderJob) {
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.telegramService = telegramService;
        this.messageSenderJob = messageSenderJob;
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            telegramService.sendAnswerCallbackQuery(answerCallbackQuery);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public ChatMember getChatMember(String chatId, int userId) {
        /*GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(chatId);
        getChatMember.setUserId(userId);

        try {
            return telegramService.execute(getChatMember);
        } catch (TelegramApiRequestException ex) {
            throw new TelegramRequestException(ex, chatId);
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }*/

        return null;
    }

    @Override
    public void sendMessageAsync(SendMessage sendMessage) {
        sendMessageAsync(sendMessage, null);
    }

    @Override
    public void sendMessageAsync(SendMessage sendMessage, Consumer<Message> callback) {
        messageSenderJob.push(() -> sendMessage0(sendMessage, callback));
    }

    @Override
    public void removeInlineKeyboardAsync(long chatId, int messageId) {
        messageSenderJob.push(() -> {
            EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
            edit.setChatId(chatId);
            edit.setMessageId(messageId);

            try {
                telegramService.editReplyMarkup(edit);
            } catch (TelegramApiException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        });
    }

    @Override
    public void editMessageAsync(EditMessageText editMessageText) {
        messageSenderJob.push(() -> {
            editMessageText.setParseMode(ParseMode.HTML);

            try {
                telegramService.editMessageText(editMessageText);
            } catch (TelegramApiException ex) {
                LOGGER.error(ex.getMessage(), ex);
                if (editMessageText.isThrowEx()) {
                    throw ex;
                }
            }
        });
    }

    @Override
    public void editMessageCaptionAsync(EditMessageCaption editMessageCaption) {
        messageSenderJob.push(new Runnable() {
            @Override
            public void run() {
                editMessageCaption.setParseMode(ParseMode.HTML);

                try {
                    telegramService.editMessageCaption(editMessageCaption);
                } catch (TelegramApiException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void editMessageMediaAsync(EditMessageMedia editMessageMedia, Consumer<EditMediaResult> consumer) {
        messageSenderJob.push(() -> {
            if (StringUtils.isNotBlank(editMessageMedia.getMedia().getCaption())) {
                editMessageMedia.getMedia().setParseMode(ParseMode.HTML);
            }
            Message message = telegramService.editMessageMedia(editMessageMedia);

            if (consumer != null) {
                consumer.accept(new EditMediaResult(fileService.getFileId(message)));
            }
        });
    }

    @Override
    public void editMessageMediaAsync(EditMessageMedia editMediaContext) {
        editMessageMediaAsync(editMediaContext, null);
    }

    @Override
    public void deleteMessageAsync(long chatId, int messageId) {
        messageSenderJob.push(() -> {
            try {
                telegramService.deleteMessage(new DeleteMessage(chatId, messageId));
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void sendDocumentAsync(SendDocument sendDocument) {
        sendDocument0(sendDocument, null);
    }

    @Override
    public void sendDocumentAsync(SendDocument sendDocument, Consumer<SendFileResult> callback) {
        messageSenderJob.push(() -> sendDocument0(sendDocument, callback));
    }

    @Override
    public void sendPhotoAsync(SendPhoto sendPhoto) {
        sendPhotoAsync(sendPhoto, null);
    }

    @Override
    public void sendPhotoAsync(SendPhoto sendPhoto, Consumer<SendFileResult> callback) {
        messageSenderJob.push(() -> {
            Message message = telegramService.sendPhoto(sendPhoto);
            if (callback != null) {
                callback.accept(new SendFileResult(message.getMessageId(), fileService.getFileId(message)));
            }
        });
    }

    @Override
    public void sendVideoAsync(SendVideo sendVideo) {
        messageSenderJob.push(() -> {
            if (StringUtils.isNotBlank(sendVideo.getCaption())) {
                sendVideo.setParseMode(ParseMode.HTML);
            }
            telegramService.sendVideo(sendVideo);
        });
    }

    @Override
    public void sendAudioAsync(SendAudio sendAudio) {
        messageSenderJob.push(() -> {
            if (StringUtils.isNotBlank(sendAudio.getCaption())) {
                sendAudio.setParseMode(ParseMode.HTML);
            }

            telegramService.sendAudio(sendAudio);
        });
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessageAsync(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return telegramService.getMediaType(fileId);
    }

    @Override
    public void sendFileAsync(long chatId, String fileId) {
        MediaType mediaType = getMediaType(fileId);

        switch (mediaType) {
            case PHOTO:
                sendPhotoAsync(new SendPhoto(chatId, fileId));
                break;
            case VIDEO:
                sendVideoAsync(new SendVideo(chatId, fileId));
                break;
            case AUDIO:
                sendAudioAsync(new SendAudio(chatId, fileId));
                break;
            default:
                sendDocumentAsync(new SendDocument(chatId, fileId));
                break;
        }
    }

    @Override
    public void sendBotRestartedMessageAsync(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessageAsync(
                new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .setReplyMarkup(replyKeyboard)
        );
    }

    @Override
    public void sendStickerAsync(SendSticker sendSticker) {
        messageSenderJob.push(() -> telegramService.sendSticker(sendSticker));
    }

    private void sendMessage0(SendMessage sendMessage, Consumer<Message> callback) {
        try {
            Message message = telegramService.sendMessage(sendMessage);

            if (callback != null) {
                callback.accept(message);
            }
        } catch (TelegramApiRequestException ex) {
            LOGGER.error("Error send message({})", sendMessage);
            throw ex;
        }
    }

    private void sendDocument0(SendDocument sendDocument, Consumer<SendFileResult> callback) {
        if (StringUtils.isNotBlank(sendDocument.getCaption())) {
            sendDocument.setParseMode(ParseMode.HTML);
        }

        Message message = telegramService.sendDocument(sendDocument);

        if (callback != null) {
            callback.accept(new SendFileResult(message.getMessageId(), fileService.getFileId(message)));
        }
    }
}
