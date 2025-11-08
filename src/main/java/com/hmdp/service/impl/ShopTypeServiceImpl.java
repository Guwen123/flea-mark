package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result getTypeList() {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String string = opsForValue.get(RedisConstants.CACHE_SHOP_TYPE_KEY);
        if (!StrUtil.isBlankIfStr(string)){
            System.out.println("从type缓存中获取数据");
            List<ShopType> list = JSONUtil.toList(string, ShopType.class);
            return Result.ok(list);
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();

        if (typeList.isEmpty()){
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_KEY, "",RedisConstants.CACHE_SHOP_TYPE_TTL + RandomUtil.randomLong(1,5), TimeUnit.MINUTES);
            return Result.fail("没有数据");

        }
        String jsonStr = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_KEY, jsonStr,RedisConstants.CACHE_SHOP_TYPE_TTL + RandomUtil.randomLong(1,5), TimeUnit.MINUTES);

        return Result.ok(typeList);
    }
}
