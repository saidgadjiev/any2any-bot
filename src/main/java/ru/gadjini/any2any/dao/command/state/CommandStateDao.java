package ru.gadjini.any2any.dao.command.state;

import java.util.Collection;

public interface CommandStateDao {
    void setState(long chatId, String command, Object state);

    <T> T getState(long chatId, String command);

    boolean hasState(long chatId, String command);

    void deleteState(long chatId, String command);

    Collection<Object> getAllStates();
}
