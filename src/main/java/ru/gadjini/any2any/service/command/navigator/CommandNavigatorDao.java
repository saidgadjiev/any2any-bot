package ru.gadjini.any2any.service.command.navigator;

public interface CommandNavigatorDao {
    void set(long chatId, String command);

    String get(long chatId);

    void pushParent(long chatId, String command);

    String popParent(long chatId, String defaultCommand);
}
