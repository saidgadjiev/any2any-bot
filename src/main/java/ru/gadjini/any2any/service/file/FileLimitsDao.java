package ru.gadjini.any2any.service.file;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class FileLimitsDao {

    private static final String KEY = "flim";

    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public FileLimitsDao(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void setInputFile(long chatId, int messageId) {
        stringRedisTemplate.opsForValue().set(key(chatId), String.valueOf(messageId));
    }

    public void setInputFileTtl(long chatId, long ttl, TimeUnit timeUnit) {
        stringRedisTemplate.expire(key(chatId), ttl, timeUnit);
    }

    public boolean hasInputFile(long chatId) {
        return BooleanUtils.toBoolean(stringRedisTemplate.hasKey(key(chatId)));
    }

    public Long getInputFileTtl(long chatId) {
        return stringRedisTemplate.getExpire(key(chatId));
    }

    public Integer getMessageId(long chatId) {
        String messageIdStr = stringRedisTemplate.opsForValue().get(key(chatId));

        if (StringUtils.isBlank(messageIdStr)) {
            return null;
        }

        return Integer.parseInt(messageIdStr);
    }

    private String key(long chatId) {
        return KEY + ":" + chatId;
    }
}
