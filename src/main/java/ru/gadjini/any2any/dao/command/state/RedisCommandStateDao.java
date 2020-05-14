package ru.gadjini.any2any.dao.command.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Qualifier("redis")
public class RedisCommandStateDao implements CommandStateDao {

    private static final String KEY = "command:state";

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisCommandStateDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setState(long chatId, Object state) {
        redisTemplate.opsForHash().put(KEY, String.valueOf(chatId), state);
    }

    @Override
    public <T> T getState(long chatId) {
        return (T) redisTemplate.opsForHash().get(KEY, String.valueOf(chatId));
    }

    @Override
    public boolean hasState(long chatId) {
        return redisTemplate.opsForHash().hasKey(KEY, String.valueOf(chatId));
    }

    @Override
    public void deleteState(long chatId) {
        redisTemplate.opsForHash().delete(KEY, String.valueOf(chatId));
    }
}
