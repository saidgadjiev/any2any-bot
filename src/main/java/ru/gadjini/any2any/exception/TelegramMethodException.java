package ru.gadjini.any2any.exception;

import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class TelegramMethodException extends RuntimeException {

    private int errorCode;

    private final String response;

    private final String chatId;

    public TelegramMethodException(TelegramApiRequestException apiException, long chatId) {
        super(apiException.getApiResponse() + "(" + chatId + ")", apiException);
        this.errorCode = apiException.getErrorCode();
        this.response = apiException.getApiResponse();
        this.chatId = String.valueOf(chatId);
    }

    public TelegramMethodException(TelegramApiRequestException apiException, String chatId) {
        super(apiException.getApiResponse() + "(" + chatId + ")", apiException);
        this.errorCode = apiException.getErrorCode();
        this.response = apiException.getApiResponse();
        this.chatId = chatId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getResponse() {
        return response;
    }

    public String getChatId() {
        return chatId;
    }
}