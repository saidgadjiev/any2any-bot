package ru.gadjini.any2any.exception;

public class DownloadCanceledException extends RuntimeException {

    public DownloadCanceledException(String message) {
        super(message);
    }
}
