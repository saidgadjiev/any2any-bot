package ru.gadjini.any2any.exception;

public class TelegramRequestException extends RuntimeException {

    private int errorCode;

    private String response;

    private long chatId;

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
