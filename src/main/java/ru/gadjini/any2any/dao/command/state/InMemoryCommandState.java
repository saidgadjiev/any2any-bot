package ru.gadjini.any2any.dao.command.state;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Qualifier("inMemory")
public class InMemoryCommandState implements CommandStateDao {

    private Map<Long, Object> states = new ConcurrentHashMap<>();

    @Override
    public void setState(long chatId, Object state) {
        states.put(chatId, state);
    }

    @Override
    public <T> T getState(long chatId) {
        return (T) states.get(chatId);
    }

    @Override
    public boolean hasState(long chatId) {
        return states.containsKey(chatId);
    }

    @Override
    public void deleteState(long chatId) {
        states.remove(chatId);
    }
}
