package com.hmdp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private static String singningKey = "TestPeo";
    private static long expiration = 4320000000L;


    /**
     * 生成Jwt令牌
     * @param claims
     * @return
     */
    public static String generateJwt(Map<String,Object> claims){

        String jwt = Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, singningKey)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .compact();
        return jwt;

    }

    /**\
     * 解析Jwt令牌
     * @param jwt
     * @return
     */
    public static Claims parseJwt(String jwt){
        Claims claims = Jwts.parser()
                .setSigningKey(singningKey)
                .parseClaimsJws(jwt)
                .getBody();
        return claims;
    }


}
