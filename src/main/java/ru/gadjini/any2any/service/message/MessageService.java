package ru.gadjini.any2any.service.message;

import ru.gadjini.any2any.model.EditMediaResult;
import ru.gadjini.any2any.model.SendFileResult;
import ru.gadjini.any2any.model.bot.api.MediaType;
import ru.gadjini.any2any.model.bot.api.method.send.*;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageCaption;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageMedia;
import ru.gadjini.any2any.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.any2any.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.any2any.model.bot.api.object.ChatMember;
import ru.gadjini.any2any.model.bot.api.object.Message;
import ru.gadjini.any2any.model.bot.api.object.replykeyboard.ReplyKeyboard;

import java.util.Locale;
import java.util.function.Consumer;

public interface MessageService {

    void sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery);

    ChatMember getChatMember(String chatId, int userId);

    void sendMessageAsync(SendMessage sendMessage);

    void sendMessageAsync(SendMessage sendMessage, Consumer<Message> callback);

    void removeInlineKeyboardAsync(long chatId, int messageId);

    void editMessageAsync(EditMessageText messageContext);

    void editMessageCaptionAsync(EditMessageCaption context);

    void editMessageMediaAsync(EditMessageMedia editMediaContext, Consumer<EditMediaResult> consumer);

    void editMessageMediaAsync(EditMessageMedia editMediaContext);

    void sendBotRestartedMessageAsync(long chatId, ReplyKeyboard replyKeyboard, Locale locale);

    void sendStickerAsync(SendSticker sendSticker);

    void deleteMessageAsync(long chatId, int messageId);

    void sendDocumentAsync(SendDocument sendDocumentContext);

    void sendDocumentAsync(SendDocument sendDocumentContext, Consumer<SendFileResult> callback);

    void sendPhotoAsync(SendPhoto sendPhoto);

    void sendPhotoAsync(SendPhoto sendPhoto, Consumer<SendFileResult> callback);

    void sendVideoAsync(SendVideo sendVideo);

    void sendAudioAsync(SendAudio sendAudio);

    void sendErrorMessage(long chatId, Locale locale);

    MediaType getMediaType(String fileId);

    void sendFileAsync(long chatId, String fileId);

}
