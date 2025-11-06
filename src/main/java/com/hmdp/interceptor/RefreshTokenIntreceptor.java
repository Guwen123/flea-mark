package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.JwtUtils;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class RefreshTokenIntreceptor implements HandlerInterceptor {
    /**
     * 前置拦截器，用于判断用户是否登录
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头中获取jwttoken
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            System.out.println("Header为空");
            return true;
        }

        Map<String, Object> claims = JwtUtils.parseJwt(token);

        // 1、判断用户是否存在
        if (claims.isEmpty()) {
            // 用户不存在，直接拦截
            System.out.println("claims为空");
            return false;
        }
        //将hashmap转DTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(claims, new UserDTO(), false);
        // 2、用户存在，则将用户信息保存到ThreadLocal中，方便后续逻辑处理
        UserHolder.saveUser(userDTO);
        System.out.println("用户存在" + userDTO.toString());

        return true;
    }
}
