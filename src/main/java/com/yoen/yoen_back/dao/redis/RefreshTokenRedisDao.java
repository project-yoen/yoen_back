package com.yoen.yoen_back.dao.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisDao {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    public void save(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                getKey(userId),
                refreshToken,
                Duration.ofDays(REFRESH_TOKEN_TTL_DAYS)
        );
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get(getKey(userId));
    }

    public void delete(String userId) {
        redisTemplate.delete(getKey(userId));
    }

    private String getKey(String userId) {
        return "refresh_token:" + userId;
    }
}
