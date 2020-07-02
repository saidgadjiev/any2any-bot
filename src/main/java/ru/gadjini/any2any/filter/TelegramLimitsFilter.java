package ru.gadjini.any2any.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.model.bot.api.method.send.SendDocument;
import ru.gadjini.any2any.model.bot.api.method.send.SendMessage;
import ru.gadjini.any2any.model.bot.api.method.send.SendSticker;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.object.Update;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

import java.util.Locale;

@Component
@Qualifier("limits")
public class TelegramLimitsFilter extends BaseBotFilter implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLimitsFilter.class);

    private static final int TEXT_LENGTH_LIMIT = 4090;

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
        messageService.sendMessage(sendMessage);
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
    public SendFileResult sendDocument(SendDocument sendDocumentContext) {
        return messageService.sendDocument(sendDocumentContext);
    }

    @Override
    public void sendErrorMessage(long chatId, Locale locale) {
        messageService.sendErrorMessage(chatId, locale);
    }
}
