package ru.gadjini.any2any.dao.command.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@Qualifier("redis")
public class RedisCommandStateDao implements CommandStateDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCommandStateDao.class);

    private static final String KEY = "cmd";

    private RedisTemplate<String, Object> redisTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public RedisCommandStateDao(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setState(long chatId, String command, Object state, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key(chatId, command), state, ttl, timeUnit);
    }

    @Override
    public <T> T getState(long chatId, String command, Class<T> tClass) {
        try {
            Object o = redisTemplate.opsForValue().get(key(chatId, command));

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
        Boolean aBoolean = redisTemplate.hasKey(key(chatId, command));

        return aBoolean != null && aBoolean;
    }

    @Override
    public void deleteState(long chatId, String command) {
        redisTemplate.delete(key(chatId, command));
    }

    @Override
    public Collection<Object> getAllStates() {
        Set<String> keys = redisTemplate.keys("cmd:");
        if (keys == null) {
            return Collections.emptyList();
        }
        Collection<Object> states = new ArrayList<>();
        for (String key : keys) {
            Object state = redisTemplate.opsForValue().get(key);
            if (state != null) {
                states.add(state);
            }
        }

        return states;
    }

    private String key(long chatId, String command) {
        return KEY + ":" + command + ":" + chatId;
    }
}
