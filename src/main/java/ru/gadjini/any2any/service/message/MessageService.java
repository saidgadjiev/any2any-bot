package ru.gadjini.any2any.service.message;
import ru.gadjini.any2any.model.*;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.util.Locale;

public interface MessageService {
    void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery);

    ChatMember getChatMember(String chatId, int userId);

    Message sendMessage(SendMessage sendMessage);

    void removeInlineKeyboard(long chatId, int messageId);

    void editMessage(EditMessageText messageContext);

    void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard);

    void editMessageCaption(EditMessageCaption context);

    EditMediaResult editMessageMedia(EditMessageMedia editMediaContext);

    void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale);

    void sendSticker(SendSticker sendSticker);

    void deleteMessage(long chatId, int messageId);

    SendFileResult sendDocument(SendDocument sendDocumentContext);

    SendFileResult sendPhoto(SendPhoto sendPhoto);

    SendFileResult sendVideo(SendVideo sendVideo);

    SendFileResult sendAudio(SendAudio sendAudio);

    void sendErrorMessage(long chatId, Locale locale);

    MediaType getMediaType(String fileId);
}
