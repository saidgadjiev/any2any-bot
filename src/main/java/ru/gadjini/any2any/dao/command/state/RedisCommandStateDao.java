package ru.gadjini.any2any.dao.command.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

@Repository
@Qualifier("redis")
public class RedisCommandStateDao implements CommandStateDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCommandStateDao.class);

    private static final String KEY = "command:state";

    private RedisTemplate<String, Object> redisTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public RedisCommandStateDao(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setState(long chatId, String command, Object state) {
        redisTemplate.opsForHash().put(KEY, key(chatId, command), state);
    }

    @Override
    public <T> T getState(long chatId, String command, Class<T> tClass) {
        try {
            Object o = redisTemplate.opsForHash().get(KEY, key(chatId, command));

            if (o == null) {
                return null;
            }
            if (o.getClass() == tClass) {
                return tClass.cast(o);
            }

            return objectMapper.convertValue(o, tClass);
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

    @Override
    public Collection<Object> getAllStates() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY);

        return entries.values();
    }

    private String key(long chatId, String command) {
        return command + ":" + chatId;
    }
}
