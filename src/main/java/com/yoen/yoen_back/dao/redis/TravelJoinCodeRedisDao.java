package com.yoen.yoen_back.dao.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TravelJoinCodeRedisDao {
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofDays(3);

    public void saveBidirectionalMapping(String code, Long travelId) {
        redisTemplate.opsForValue().set("joinCode:" + code, String.valueOf(travelId), TTL);
        redisTemplate.opsForValue().set("travelCode:" + travelId, code, TTL);
    }

    public Optional<String> getTripIdByCode(String code) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("joinCode:" + code));
    }

    public Optional<String> getCodeByTravelId(Long travelId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("travelCode:" + travelId));
    }

    public void deleteByCode(String code) {
        String travelId = redisTemplate.opsForValue().get("joinCode:" + code);
        if (travelId != null) {
            redisTemplate.delete("travelCode:" + travelId);
        }
        redisTemplate.delete("joinCode:" + code);
    }

    public void deleteByTravelId(Long travelId) {
        String code = redisTemplate.opsForValue().get("travelCode:" + travelId);
        if (code != null) {
            redisTemplate.delete("joinCode:" + code);
        }
        redisTemplate.delete("travelCode:" + travelId);
    }

    public boolean existsCode(String code) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("joinCode:" + code));
    }

    public boolean existsTravelId(Long travelId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("travelCode:" + travelId));
    }

    public Optional<LocalDateTime> getExpirationTime(String code) {
        Long seconds = redisTemplate.getExpire("joinCode:" + code, TimeUnit.SECONDS);
        if (seconds == null || seconds < 0) {
            return Optional.empty();
        }
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(seconds);
        return Optional.of(expireAt);
    }
}
