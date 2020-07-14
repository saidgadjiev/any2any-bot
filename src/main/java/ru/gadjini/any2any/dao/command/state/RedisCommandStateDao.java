package ru.gadjini.any2any.dao.command.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Qualifier("redis")
public class RedisCommandStateDao implements CommandStateDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCommandStateDao.class);

    private static final String KEY = "command:state";

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisCommandStateDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setState(long chatId, String command, Object state) {
        redisTemplate.opsForHash().put(KEY, key(chatId, command), state);
    }

    @Override
    public <T> T getState(long chatId, String command) {
        try {
            return (T) redisTemplate.opsForHash().get(KEY, key(chatId, command));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean hasState(long chatId, String command) {
        return redisTemplate.opsForHash().hasKey(KEY, key(chatId, command));
    }

    @Override
    public void deleteState(long chatId, String command) {
        redisTemplate.opsForHash().delete(KEY, key(chatId, command));
    }

    private String key(long chatId, String command) {
        return command + ":" + chatId;
    }
}
