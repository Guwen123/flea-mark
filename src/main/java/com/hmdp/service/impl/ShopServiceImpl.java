package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result getShopById(Long id) {
        // 缓存穿透
        // Shop shop = cacheClient.getShopByIdWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomLong(1,5), TimeUnit.MINUTES)

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient.getShopByIdWithMutex(CACHE_SHOP_KEY,id,Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomLong(1,5), TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.getShopByIdWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class, this::getById, CACHE_SHOP_TTL + RandomUtil.randomLong(1,5), TimeUnit.MINUTES);

        if (shop == null){
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }
}
