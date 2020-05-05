package ru.gadjini.any2any.service.command.navigator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Qualifier("inMemory")
public class InMemoryNavigatorDao implements CommandNavigatorDao {

    private Map<Long, String> commands = new ConcurrentHashMap<>();

    private Map<Long, String> parentCommands = new ConcurrentHashMap<>();

    @Override
    public void set(long chatId, String command) {
        commands.put(chatId, command);
    }

    @Override
    public String get(long chatId) {
        return commands.get(chatId);
    }

    @Override
    public void pushParent(long chatId, String command) {
        parentCommands.put(chatId, command);
    }

    @Override
    public String popParent(long chatId, String defaultCommand) {
        return parentCommands.get(chatId);
    }
}
