package ru.gadjini.any2any.service.message;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.ParseMode;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;

import java.util.Locale;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final String API = "http://localhost:5005/";

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
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(AnswerCallbackQuery.METHOD), request, Boolean.class);
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
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(HtmlMessage.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(new EditMessageReplyMarkup(chatId, messageId));
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(EditMessageReplyMarkup.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public void editMessage(EditMessageText editMessageText) {
        editMessageText.setParseMode(ParseMode.HTML);
        HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(EditMessageText.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard) {
        HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(new EditMessageReplyMarkup(chatId, messageId)
                .setReplyMarkup(replyKeyboard));
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(EditMessageText.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public void editMessageCaption(EditMessageCaption editMessageCaption) {
        editMessageCaption.setParseMode(ParseMode.HTML);
        HttpEntity<EditMessageCaption> request = new HttpEntity<>(editMessageCaption);
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(EditMessageCaption.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public EditMediaResult editMessageMedia(EditMessageMedia editMessageMedia) {
        if (StringUtils.isNotBlank(editMessageMedia.getMedia().getCaption())) {
            editMessageMedia.getMedia().setParseMode(ParseMode.HTML);
        }
        HttpEntity<EditMessageMedia> request = new HttpEntity<>(editMessageMedia);
        ResponseEntity<Message> responseEntity = restTemplate.postForEntity(getUrl(EditMessageMedia.METHOD), request, Message.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
            throw new TelegramRequestException(responseEntity.getStatusCode().value());
        }

        return new EditMediaResult(fileService.getFileId(responseEntity.getBody()));
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessage(
                new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .setReplyMarkup(replyKeyboard)
        );
    }

    @Override
    public void sendSticker(SendSticker sendSticker) {
        HttpEntity<SendSticker> request = new HttpEntity<>(sendSticker);
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(SendSticker.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
            throw new TelegramRequestException(responseEntity.getStatusCode().value());
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        HttpEntity<DeleteMessage> request = new HttpEntity<>(new DeleteMessage(chatId, messageId));
        ResponseEntity responseEntity = restTemplate.postForEntity(getUrl(DeleteMessage.METHOD), request, Void.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCode().value());
        }
    }

    @Override
    public SendFileResult sendDocument(SendDocument sendDocument) {
        if (StringUtils.isNotBlank(sendDocument.getCaption())) {
            sendDocument.setParseMode(ParseMode.HTML);
        }

        HttpEntity<SendDocument> request = new HttpEntity<>(sendDocument);
        ResponseEntity<Message> responseEntity = restTemplate.postForEntity(getUrl(SendDocument.METHOD), request, Message.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            LOGGER.error("Code: {}", responseEntity.getStatusCodeValue());
            throw new TelegramRequestException(responseEntity.getStatusCodeValue());
        }

        Message message = responseEntity.getBody();
        return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }

    private String getUrl(String method) {
        return API + method;
    }
}
