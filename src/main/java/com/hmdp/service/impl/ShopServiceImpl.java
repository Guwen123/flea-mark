package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 判断是否需要查询坐标范围
        if (x == null || y == null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 页数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 获取key对应的geo
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key, GeoReference.fromCoordinate(x, y), new Distance(5000)
                        , RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end));
        // 解析geo
        if (results == null){
            return Result.ok();
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        // 劫取from到end
        List<Long> ids = new ArrayList<>();
        Map<String, Distance> map = new HashMap<>();
        if (list.size() <=  from){
            return Result.ok();
        }
        list.stream().skip(from).forEach(result -> {
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));

            Distance distance = result.getDistance();
            map.put(shopId, distance);
        });
        // 从根据shop的id数据库获取Shop
        List<Shop> shops = query().in("id", ids)
                .last("ORDER BY FIELD(id," + StrUtil.join(",", ids) + ")").list();
        shops.forEach(shop -> {
            shop.setDistance(map.get(shop.getId().toString()).getValue());
        });

        return Result.ok(shops);

    }
}
