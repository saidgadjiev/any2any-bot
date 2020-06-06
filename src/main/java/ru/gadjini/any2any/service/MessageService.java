package ru.gadjini.any2any.service;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.any2any.model.EditMediaContext;
import ru.gadjini.any2any.model.EditMessageContext;
import ru.gadjini.any2any.model.SendFileContext;
import ru.gadjini.any2any.model.SendMessageContext;

import java.io.File;
import java.util.Locale;

public interface MessageService {
    ChatMember getChatMember(String chatId, int userId);

    void sendMessage(SendMessageContext messageContext);

    void removeInlineKeyboard(long chatId, int messageId);

    void editMessage(EditMessageContext messageContext);

    void editReplyKeyboard(long chatId, int messageId, InlineKeyboardMarkup replyKeyboard);

    void editMessageMedia(EditMediaContext editMediaContext);

    void sendBotRestartedMessage(long chatId, ReplyKeyboard replyKeyboard, Locale locale);

    void sendSticker(SendFileContext sendFileContext);

    void deleteMessage(long chatId, int messageId);

    int sendDocument(SendFileContext sendDocumentContext);

    int sendPhoto(SendFileContext sendDocumentContext);

    void sendErrorMessage(long chatId, Locale locale);
}
