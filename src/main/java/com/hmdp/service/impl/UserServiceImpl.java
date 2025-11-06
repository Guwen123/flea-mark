package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.JwtUtils;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1、判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // 2、手机号合法，生成验证码，并保存到Session/Redis中
        String code = RandomUtil.randomNumbers(6);
        // session.setAttribute("code", code);
        stringRedisTemplate.opsForValue().set("login:code" + phone, code,
                2, TimeUnit.MINUTES);//有效期两分钟
        // 3、发送验证码
        log.info("验证码:{}", code);
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        // 1、判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // 2、判断验证码是否正确
        String redisCode=stringRedisTemplate.opsForValue().get("login:code"+phone);
//        String sessionCode = (String) session.getAttribute("code");

        if (redisCode == null || !redisCode.equals(code)) {
            return Result.fail("验证码不正确");
        }
        // 3、判断手机号是否是已存在的用户
        User user =query().eq("phone",phone).one();
        if (user == null) {
            // 用户不存在，需要注册
            user = createUserWithPhone(phone);
            log.info("用户不存在，注册用户:{}",user);
        }
        // 4、保存用户信息到Session/Redis中，便于后面逻辑的判断（比如登录判断、随时取用户信息，减少对数据库的查询）
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        //改进，使用jwt令牌
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("id",userDTO.getId());
        claims.put("nickName",userDTO.getNickName());
        claims.put("icon",userDTO.getIcon());
        String jwtToken = JwtUtils.generateJwt(claims);

        return Result.ok(jwtToken);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone( phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
