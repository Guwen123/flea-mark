package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long COUNT_BIT = 32;
    private static final long BEGIN_TIMESTAMP = 1762616268L;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix) {
        // 获取当前时间戳你
        LocalDateTime now = LocalDateTime.now();
        long newSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = newSecond - BEGIN_TIMESTAMP;

        // 生成序列号
        String DateToDay = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + DateToDay);

        return timeStamp << COUNT_BIT | increment;
    }
}
