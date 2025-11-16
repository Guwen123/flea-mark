package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followId, Boolean isFollow) {
        Long id = UserHolder.getUser().getId();

        if (isFollow){
            // isFollow为true,是关注
            Follow follow = new Follow();
            follow.setUserId(id);
            follow.setFollowUserId(followId);
            follow.setCreateTime(LocalDateTime.now());
            boolean isSuccess = save(follow);
            if (!isSuccess){
                return Result.fail("关注失败");
            }
            String key = RedisConstants.USER_FOLLOW_KEY + id;
            stringRedisTemplate.opsForSet().add(key, followId.toString());
        }else {
            // 为false 取关
            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id", id)
                    .eq("follow_user_id", followId));
            if (!isSuccess){
                return Result.fail("取关失败");
            }
            String key = RedisConstants.USER_FOLLOW_KEY + id;
            stringRedisTemplate.opsForSet().remove(key, followId.toString());
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followId).count();

        return Result.ok(count > 0);
    }

    @Override
    public Result common(Long followId) {
        Long userId = UserHolder.getUser().getId();
        String key1 = RedisConstants.USER_FOLLOW_KEY + userId;
        String key2 = RedisConstants.USER_FOLLOW_KEY + followId;
        // 交际
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (set == null || set.isEmpty()){
            return Result.ok();
        }
        // user转化成userDTO
        List<Long> list = set.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOS = userService.listByIds(list).stream().map(user -> {
            return BeanUtil.copyProperties(user, UserDTO.class);
        }).collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}
