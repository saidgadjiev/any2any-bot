package ru.gadjini.any2any.exception;

import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class TelegramRequestException extends RuntimeException {

    private int errorCode;

    private final String response;

    private long chatId;

    public TelegramRequestException(TelegramApiRequestException apiException, long chatId) {
        super(apiException.getApiResponse() + "(" + chatId + ") error code " + apiException.getErrorCode() + ". " + apiException.getMessage(),
                apiException);
        this.errorCode = apiException.getErrorCode();
        this.response = apiException.getApiResponse();
        this.chatId = chatId;
    }

    public TelegramRequestException(TelegramApiRequestException apiException, String chatId) {
        super(apiException.getApiResponse() + "(" + chatId + ") error code " + apiException.getErrorCode() + ". " + apiException.getMessage(), apiException);
        this.errorCode = apiException.getErrorCode();
        this.response = apiException.getApiResponse();
        this.chatId = -1;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getResponse() {
        return response;
    }

    public long getChatId() {
        return chatId;
    }
}
