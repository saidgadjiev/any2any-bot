package ru.gadjini.any2any.exception;

public class UserException extends RuntimeException {

    private String humanMessage;

    private boolean printLog = false;

    public UserException(String humanMessage) {
        super(humanMessage);
        this.humanMessage = humanMessage;
    }

    public UserException(String humanMessage, boolean printLog) {
        super(humanMessage);
        this.humanMessage = humanMessage;
        this.printLog = printLog;
    }

    public String getHumanMessage() {
        return humanMessage;
    }

    public boolean isPrintLog() {
        return printLog;
    }
}
