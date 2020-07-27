package ru.gadjini.any2any.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.model.Any2AnyFile;

@Repository
public class BotSettingsDao {

    private static final String KEY = "bot:settings";

    private static final String THUMB = "thumb";

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public BotSettingsDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void deleteThumb(long chatId) {
        redisTemplate.opsForHash().delete(key(chatId), THUMB);
    }

    public void setThumb(long chatId, Any2AnyFile thumb) {
        redisTemplate.opsForHash().put(key(chatId), THUMB, thumb);
    }

    public Any2AnyFile getThumb(long chatId) {
        return (Any2AnyFile) redisTemplate.opsForHash().get(key(chatId), THUMB);
    }

    private String key(long chatId) {
        return KEY + ":" + chatId;
    }
}
