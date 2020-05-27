package ru.gadjini.any2any.dao.command.state;

public interface CommandStateDao {
    void setState(long chatId, String command, Object state);

    <T> T getState(long chatId, String command);

    boolean hasState(long chatId, String command);

    void deleteState(long chatId, String command);
}
