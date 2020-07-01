package ru.gadjini.any2any.service.message;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.method.SendMessage;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.util.Locale;

public interface MessageService {
    void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery);

    ChatMember getChatMember(String chatId, int userId);

    void sendMessage(SendMessage sendMessage);

    void removeInlineKeyboard(long chatId, int messageId);

    void editMessage(EditMessageContext messageContext);

    void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard);

    void editMessageCaption(EditMessageCaptionContext context);

    EditMediaResult editMessageMedia(EditMediaContext editMediaContext);

    void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale);

    void sendSticker(SendFileContext sendFileContext);

    void deleteMessage(long chatId, int messageId);

    SendFileResult sendDocument(SendFileContext sendDocumentContext);

    int sendPhoto(SendFileContext sendDocumentContext);

    void sendErrorMessage(long chatId, Locale locale);
}
