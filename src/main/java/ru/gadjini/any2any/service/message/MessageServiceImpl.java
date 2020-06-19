package ru.gadjini.any2any.service.message;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TelegramException;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.service.FileService;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.TelegramService;

import java.util.Locale;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private TelegramService telegramService;

    private LocalisationService localisationService;

    private FileService fileService;

    @Autowired
    public MessageServiceImpl(TelegramService telegramService, LocalisationService localisationService, FileService fileService) {
        this.telegramService = telegramService;
        this.localisationService = localisationService;
        this.fileService = fileService;
    }

    @Override
    public void sendAnswerCallbackQuery(AnswerCallbackContext callbackContext) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();

        answerCallbackQuery.setText(callbackContext.text());
        answerCallbackQuery.setCallbackQueryId(callbackContext.queryId());

        try {
            telegramService.execute(answerCallbackQuery);
        } catch (TelegramApiRequestException ex) {
            LOGGER.error(ex.getApiResponse(), ex);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public ChatMember getChatMember(String chatId, int userId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(chatId);
        getChatMember.setUserId(userId);

        try {
            return telegramService.execute(getChatMember);
        } catch (TelegramApiRequestException ex) {
            throw new TelegramRequestException(ex, chatId);
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }
    }

    @Override
    public void sendMessage(SendMessageContext messageContext) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(messageContext.chatId());
        sendMessage.enableHtml(messageContext.html());
        sendMessage.setText(messageContext.text());

        if (!messageContext.webPagePreview()) {
            sendMessage.disableWebPagePreview();
        }

        if (messageContext.hasKeyboard()) {
            sendMessage.setReplyMarkup(messageContext.replyKeyboard());
        }

        if (messageContext.replyMessageId() != null) {
            sendMessage.setReplyToMessageId(messageContext.replyMessageId());
        }

        try {
            telegramService.execute(sendMessage);
        } catch (TelegramApiRequestException ex) {
            LOGGER.error("Error send message. With context: " + messageContext.toString());
            throw new TelegramRequestException(ex, messageContext.chatId());
        } catch (Exception ex) {
            throw new TelegramException(ex);
        }
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);

        try {
            telegramService.execute(edit);
        } catch (TelegramApiException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void editMessage(EditMessageContext messageContext) {
        EditMessageText editMessageText = new EditMessageText();

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
        }
    }

    @Override
    public void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();

        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setReplyMarkup(replyKeyboard);

        try {
            telegramService.execute(editMessageReplyMarkup);
        } catch (TelegramApiException ex) {
            throw new TelegramException(ex);
        }
    }

    @Override
    public void editMessageCaption(EditMessageCaptionContext context) {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(String.valueOf(context.chatId()));
        editMessageCaption.setMessageId(context.messageId());
        editMessageCaption.setCaption(context.caption());
        editMessageCaption.setParseMode("html");

        if (context.replyKeyboard() != null) {
            editMessageCaption.setReplyMarkup(context.replyKeyboard());
        }

        try {
            telegramService.execute(editMessageCaption);
        } catch (TelegramApiRequestException apiException) {
            LOGGER.error(apiException.getApiResponse() + "(" + context.chatId() + ") error code " + apiException.getErrorCode() + ". " + apiException.getMessage(), apiException);
        } catch (TelegramApiException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public EditMediaResult editMessageMedia(EditMediaContext editMediaContext) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
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
            document.setParseMode("html");
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
        }
    }

    @Override
    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessage(
                new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .replyKeyboard(replyKeyboard)
        );
    }

    @Override
    public void sendSticker(SendFileContext sendFileContext) {
        SendSticker sendSticker = new SendSticker();
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
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            telegramService.execute(deleteMessage);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public SendFileResult sendDocument(SendFileContext sendDocumentContext) {
        SendDocument sendDocument = new SendDocument();
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
            sendDocument.setParseMode("html");
        }

        try {
            Message message = telegramService.execute(sendDocument);

            return new SendFileResult(message.getMessageId(), fileService.getFileId(message));
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new TelegramRequestException(e, sendDocumentContext.chatId());
        } catch (TelegramApiException e) {
            throw new TelegramException(e);
        }
    }

    @Override
    public int sendPhoto(SendFileContext sendFileContext) {
        SendPhoto sendPhoto = new SendPhoto();
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
        }
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }
}
