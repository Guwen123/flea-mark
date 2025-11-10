package com.hmdp.utils;
import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RedisLock {
    private StringRedisTemplate stringRedisTemplate;
    private final String KEY_PREFIX = "lock:";
    private long key;
    private String uuid = UUID.randomUUID().toString(true);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua_script.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public RedisLock(StringRedisTemplate stringRedisTemplate, long key) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
    }
    public boolean tryLock(long timeout) {
        String name = Thread.currentThread().getName();

        Boolean flag = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + key, uuid+ "_" +name , timeout , TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

//    public void unLock( ) {
//        String name = Thread.currentThread().getName();
//        String threadId1 = uuid+ "_" + name;
//        String threadId2 = stringRedisTemplate.opsForValue().get(KEY_PREFIX + key);
//
//        if (threadId1.equals(threadId2)){
//            stringRedisTemplate.delete(KEY_PREFIX + key);
//        }
//    }
    public void unLock( ) {
        String name = Thread.currentThread().getName();
        List<String> keys = Collections.singletonList(uuid + "_" + name);

        stringRedisTemplate.execute(UNLOCK_SCRIPT,keys, uuid+ "_" +name);
    }
}
