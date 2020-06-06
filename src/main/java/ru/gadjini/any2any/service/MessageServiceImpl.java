package ru.gadjini.any2any.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.exception.TelegramMethodException;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.EditMessageContext;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendMessageContext;

import java.util.Locale;

@Service
@Qualifier("message")
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private TelegramService telegramService;

    private LocalisationService localisationService;

    @Autowired
    public MessageServiceImpl(TelegramService telegramService, LocalisationService localisationService) {
        this.telegramService = telegramService;
        this.localisationService = localisationService;
    }

    @Override
    public ChatMember getChatMember(String chatId, int userId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(chatId);
        getChatMember.setUserId(userId);

        try {
            return telegramService.execute(getChatMember);
        } catch (TelegramApiRequestException ex) {
            throw new TelegramMethodException(ex, chatId);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void editMessageMedia(EditMediaContext editMediaContext) {
        EditMessageMedia editMessageMedia = new EditMessageMedia();
        editMessageMedia.setChatId(editMediaContext.chatId());
        editMessageMedia.setMessageId(editMediaContext.messageId());
        InputMediaDocument document = new InputMediaDocument();
        document.setMedia(editMediaContext.file(), editMediaContext.file().getName());
        document.setCaption(editMediaContext.caption());
        editMessageMedia.setMedia(document);

        if (editMediaContext.replyKeyboard() != null) {
            editMessageMedia.setReplyMarkup(editMediaContext.replyKeyboard());
        }

        try {
            telegramService.execute(editMessageMedia);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
    public int sendDocument(SendFileContext sendDocumentContext) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(sendDocumentContext.chatId());
        sendDocument.setDocument(sendDocumentContext.file());

        if (sendDocumentContext.replyMessageId() != null) {
            sendDocument.setReplyToMessageId(sendDocumentContext.replyMessageId());
        }
        if (sendDocumentContext.replyKeyboard() != null) {
            sendDocument.setReplyMarkup(sendDocumentContext.replyKeyboard());
        }

        try {
            return telegramService.execute(sendDocument).getMessageId();
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new RuntimeException(e);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }
}
