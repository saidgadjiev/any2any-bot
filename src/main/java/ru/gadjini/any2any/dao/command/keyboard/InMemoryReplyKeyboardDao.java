package ru.gadjini.any2any.dao.command.keyboard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Qualifier("inMemory")
public class InMemoryReplyKeyboardDao implements ReplyKeyboardDao {

    private Map<Long, ReplyKeyboardMarkup> cache = new ConcurrentHashMap<>();

    @Override
    public void store(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        cache.put(chatId, replyKeyboardMarkup);
    }

    @Override
    public ReplyKeyboardMarkup get(long chatId) {
        return cache.get(chatId);
    }
}
