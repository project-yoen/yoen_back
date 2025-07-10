package com.yoen.yoen_back.dao.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisDao {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long REFRESH_TOKEN_TTL_DAYS = 14;

    public void save(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                "refresh_token:" + userId,
                refreshToken,
                Duration.ofDays(REFRESH_TOKEN_TTL_DAYS)
        );
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get("refresh_token:" + userId);
    }

    public void delete(String userId) {
        redisTemplate.delete("refresh_token:" + userId);
    }

}
