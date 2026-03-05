package com.eolma.user.adapter.out.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuthLinkTokenStore {

    private static final String KEY_PREFIX = "oauth_link:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public OAuthLinkTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createToken(String email) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, email, TTL);
        return token;
    }

    public Optional<String> consumeToken(String token) {
        String key = KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);
        if (email != null) {
            redisTemplate.delete(key);
        }
        return Optional.ofNullable(email);
    }
}
