package ru.gadjini.any2any.dao.command.state;

public interface CommandStateDao {
    void setState(long chatId, Object state);

    <T> T getState(long chatId);

    boolean hasState(long chatId);

    void deleteState(long chatId);
}
