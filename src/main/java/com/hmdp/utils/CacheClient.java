package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    public <R,ID> R getShopByIdWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> function,Long time, TimeUnit unit) {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String string = opsForValue.get(keyPrefix + id);
        if (StrUtil.isNotBlank(string)){
            System.out.println("从缓存中获取数据");
            return JSONUtil.toBean(string, type);
        }
        if (string != null){
            return null;
        }
        R r = function.apply(id);
        long addTime = RandomUtil.randomLong(1, 5);
        if (r == null){
            this.set(keyPrefix + id, "", time + addTime, unit);
            return null;
        }
        this.set(keyPrefix + id, r, time + addTime, unit);

        return r;
    }



    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R getShopByIdWithLogicalExpire(String keyPrefix,ID id, Class<R> type, Function<ID, R> function, Long time, TimeUnit unit) {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String string = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        if (StrUtil.isBlank(string)){
            // 未命中
            R apply = function.apply(id);
            RedisData redisData = new RedisData();
            redisData.setData(apply);
            redisData.setExpireTime(LocalDateTime.now().plusHours(unit.toHours(time)));

            if (apply == null){
                this.set(keyPrefix + id, "", time, unit);
                return null;
            }
            this.set(keyPrefix + id, redisData, time, unit);
            return apply;
        }
        // 命中
        RedisData redisData = JSONUtil.toBean(string, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())){
            // 未过期返回旧数据
            return r;
        }
        // 过期
        String lock_key = keyPrefix + id;
        boolean lock = lock(lock_key);
        // 未获得锁,返回旧数据
        if (!lock){
            return r;
        }
        // 获得锁,开启一个线程执行重建过程
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                R r1 = function.apply(id);
                this.set(keyPrefix + id, r1, time, unit);
            } finally {
                // 重构完成，释放锁
                unlock(lock_key);
            }
        });

        return r;
    }

    public <R,ID> R getShopByIdWithMutex(String keyPrefix,ID id, Class<R> type,Function<ID,R> function, Long time, TimeUnit unit) {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String string = opsForValue.get(keyPrefix + id);
        if (!StrUtil.isBlankIfStr(string)){
            System.out.println("从缓存中获取数据");
            return JSONUtil.toBean(string, type);
        }
        // 缓存重建
        R r = null;
        String lock_key = keyPrefix + id;
        try {
            boolean lock = lock(lock_key);
            if (!lock){
                Thread.sleep(50);
                return getShopByIdWithMutex(keyPrefix,id,type,function,time,unit);
            }
            r = function.apply(id);
            String str = JSONUtil.toJsonStr(r);
            if (r == null){
                opsForValue.set(lock_key, "" ,time, unit);
                return r;
            }
            opsForValue.set(lock_key, str ,time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lock_key);
        }

        return r;
    }


    private boolean lock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
