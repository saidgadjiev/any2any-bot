package ru.gadjini.any2any.service.message;

import com.aspose.imaging.internal.fX.H;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.method.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private LocalisationService localisationService;

    private FileService fileService;

    private RestTemplate restTemplate;

    @Autowired
    public MessageServiceImpl(LocalisationService localisationService, FileService fileService) {
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        HttpEntity<AnswerCallbackQuery> request = new HttpEntity<>(answerCallbackQuery);
        ResponseEntity responseEntity = restTemplate.postForEntity("http://localhost:5005/answercallbackquery", request, Boolean.class);
        LOGGER.debug(responseEntity.getStatusCode().name());
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
    public void sendMessage(SendMessage sendMessage) {
        HttpEntity<SendMessage> request = new HttpEntity<>(sendMessage);
        ResponseEntity responseEntity = restTemplate.postForEntity("http://localhost:5000/sendmessage", request, Void.class);

        LOGGER.debug(responseEntity.getStatusCode().name());
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        /*EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);

        try {
            telegramService.execute(edit);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }*/
    }

    @Override
    public void editMessage(EditMessageContext messageContext) {
        /*EditMessageText editMessageText = new EditMessageText();

        editMessageText.setMessageId(messageContext.messageId());
        editMessageText.enableHtml(true);
        editMessageText.setChatId(messageContext.chatId());
        editMessageText.setText(messageContext.text());
        if (messageContext.hasKeyboard()) {
            editMessageText.setReplyMarkup(messageContext.replyKeyboard());
        }

        try {
            telegramService.execute(editMessageText);
        } catch (TelegramApiRequestException ex) {
            LOGGER.error(ex.getApiResponse(), ex);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }*/
    }

    @Override
    public void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard) {
        /*EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();

        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setReplyMarkup(replyKeyboard);

        try {
            telegramService.execute(editMessageReplyMarkup);
        } catch (TelegramApiException ex) {
            throw new TelegramException(ex);
        }*/
    }

    @Override
    public void editMessageCaption(EditMessageCaptionContext context) {
       /* EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(String.valueOf(context.chatId()));
        editMessageCaption.setMessageId(context.messageId());
        editMessageCaption.setCaption(context.caption());
        editMessageCaption.setParseMode("enableHtml");

        if (context.replyKeyboard() != null) {
            editMessageCaption.setReplyMarkup(context.replyKeyboard());
        }

        try {
            telegramService.execute(editMessageCaption);
        } catch (TelegramApiRequestException apiException) {
            LOGGER.error(apiException.getApiResponse() + "(" + context.chatId() + ") error code " + apiException.getErrorCode() + ". " + apiException.getMessage(), apiException);
        } catch (TelegramApiException e) {
            LOGGER.error(e.getMessage(), e);
        }*/
    }

    @Override
    public EditMediaResult editMessageMedia(EditMediaContext editMediaContext) {
        /*EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setChatId(editMediaContext.chatId());
        editMessageMedia.setMessageId(editMediaContext.messageId());
        InputMediaDocument document = new InputMediaDocument();
        if (editMediaContext.file() != null) {
            document.setMedia(editMediaContext.file(), editMediaContext.file().getName());
        } else if (editMediaContext.fileId() != null) {
            document.setMedia(editMediaContext.fileId());
        }
        if (editMediaContext.caption() != null) {
            document.setCaption(editMediaContext.caption());
            document.setParseMode("enableHtml");
        }
        editMessageMedia.setMedia(document);

        if (editMediaContext.replyKeyboard() != null) {
            editMessageMedia.setReplyMarkup(editMediaContext.replyKeyboard());
        }

        try {
            Message message = (Message) telegramService.execute(editMessageMedia);

            return new EditMediaResult(fileService.getFileId(message));
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }*/

        return null;
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessage(
                new SendMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .setReplyMarkup(replyKeyboard)
        );
    }

    @Override
    public void sendSticker(SendFileContext sendFileContext) {
        /*SendSticker sendSticker = new SendSticker();
        sendSticker.setSticker(sendFileContext.file());
        sendSticker.setChatId(sendFileContext.chatId());

        if (sendFileContext.replyMessageId() != null) {
            sendSticker.setReplyToMessageId(sendFileContext.replyMessageId());
        }
        if (sendFileContext.replyKeyboard() != null) {
            sendSticker.setReplyMarkup(sendFileContext.replyKeyboard());
        }

        try {
            telegramService.execute(sendSticker);
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new TelegramRequestException(e, sendFileContext.chatId());
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }*/
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        /*DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            telegramService.execute(deleteMessage);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }*/
    }

    @Override
    public SendFileResult sendDocument(SendFileContext sendDocumentContext) {
        /*SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(sendDocumentContext.chatId());
        if (sendDocumentContext.file() != null) {
            sendDocument.setDocument(sendDocumentContext.file());
        } else {
            sendDocument.setDocument(sendDocumentContext.fileId());
        }

        if (sendDocumentContext.replyMessageId() != null) {
            sendDocument.setReplyToMessageId(sendDocumentContext.replyMessageId());
        }
        if (sendDocumentContext.replyKeyboard() != null) {
            sendDocument.setReplyMarkup(sendDocumentContext.replyKeyboard());
        }
        if (StringUtils.isNotBlank(sendDocumentContext.caption())) {
            sendDocument.setCaption(sendDocumentContext.caption());
            sendDocument.setParseMode("enableHtml");
        }

        try {
            Message message = telegramService.execute(sendDocument);

            return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new TelegramRequestException(e, sendDocumentContext.chatId());
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }*/

        return null;
    }

    @Override
    public int sendPhoto(SendFileContext sendFileContext) {
        /*SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(sendFileContext.chatId());
        sendPhoto.setPhoto(sendFileContext.file());

        if (sendFileContext.replyMessageId() != null) {
            sendPhoto.setReplyToMessageId(sendFileContext.replyMessageId());
        }
        if (sendFileContext.replyKeyboard() != null) {
            sendPhoto.setReplyMarkup(sendFileContext.replyKeyboard());
        }

        try {
            return telegramService.execute(sendPhoto).getMessageId();
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new TelegramRequestException(e, sendFileContext.chatId());
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }*/
        return 0;
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new SendMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }
}
