package com.eolma.user.adapter.out.redis;

import com.eolma.user.config.JwtConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final Duration refreshTokenTtl;

    public RefreshTokenStore(StringRedisTemplate redisTemplate, JwtConfig jwtConfig) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtl = Duration.ofMillis(jwtConfig.getRefreshTokenExpiration());
    }

    public void save(String memberId, String refreshToken) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + memberId,
                refreshToken,
                refreshTokenTtl
        );
    }

    public Optional<String> find(String memberId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(KEY_PREFIX + memberId)
        );
    }

    public void delete(String memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
