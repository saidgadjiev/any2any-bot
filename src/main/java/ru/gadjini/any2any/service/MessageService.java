package ru.gadjini.any2any.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.model.SendDocumentContext;
import ru.gadjini.any2any.model.SendMessageContext;

import java.util.Locale;

@Service
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    private TelegramService telegramService;

    private LocalisationService localisationService;

    @Autowired
    public MessageService(TelegramService telegramService, LocalisationService localisationService) {
        this.telegramService = telegramService;
        this.localisationService = localisationService;
    }

    public void sendMessage(SendMessageContext messageContext) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(messageContext.chatId());
        sendMessage.enableHtml(messageContext.html());
        sendMessage.setText(messageContext.text());
        sendMessage.disableWebPagePreview();

        if (messageContext.hasKeyboard()) {
            sendMessage.setReplyMarkup(messageContext.replyKeyboard());
        }

        try {
            telegramService.execute(sendMessage);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale) {
        sendMessage(
                new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_BOT_RESTARTED, locale))
                        .replyKeyboard(replyKeyboard)
        );
    }

    public void sendDocument(SendDocumentContext sendDocumentContext) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(sendDocumentContext.chatId());
        sendDocument.setDocument(sendDocumentContext.file());

        if (sendDocumentContext.replyMessageId() != null) {
            sendDocument.setReplyToMessageId(sendDocumentContext.replyMessageId());
        }

        try {
            telegramService.execute(sendDocument);
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage() + ". " + e.getApiResponse() + "(" + e.getErrorCode() + "). Params " + e.getParameters(), e);
            throw new RuntimeException(e);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendErrorMessage(long chatId, Locale locale) {
        sendMessage(new SendMessageContext(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_ERROR, locale)));
    }
}
