package ru.gadjini.any2any.exception;

public class UserException extends RuntimeException {

    private String humanMessage;

    public UserException(String humanMessage) {
        this.humanMessage = humanMessage;
    }

    public String getHumanMessage() {
        return humanMessage;
    }
}
