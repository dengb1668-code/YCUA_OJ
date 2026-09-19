package com.oj.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具: 生成/解析登录令牌
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${oj.jwt.secret}") String secret,
                   @Value("${oj.jwt.expire-hours:168}") long expireHours) {
        // HS256 要求密钥至少 32 字节
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token, 无效/过期抛异常
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long parseUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    /**
     * 签发比赛访问 token(join 比赛成功后发放, 用于比赛题目/榜单/提交接口):
     * claims 带 type=contest 与 cid, 与登录 token 严格区分
     */
    public String generateContestToken(Long contestId, Long userId, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "contest")
                .claim("cid", contestId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析比赛 token: 校验签名/过期且 type=contest, 返回 cid
     */
    public Long parseContestToken(String token) {
        Claims claims = parse(token);
        if (!"contest".equals(claims.get("type"))) {
            throw new IllegalArgumentException("非比赛访问令牌");
        }
        Object cid = claims.get("cid");
        return cid == null ? null : Long.valueOf(String.valueOf(cid));
    }
}
