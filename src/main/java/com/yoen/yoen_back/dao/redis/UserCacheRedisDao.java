package com.yoen.yoen_back.dao.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.dto.user.CachedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * JWT 인증 필터의 요청당 유저 DB 조회를 줄이기 위한 캐시.
 * 유저 정보가 바뀌는 곳(UserService.updateUser 등)에서 반드시 evict 해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheRedisDao {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(10);

    public void save(CachedUser user) {
        try {
            redisTemplate.opsForValue().set(key(user.userId()), objectMapper.writeValueAsString(user), USER_CACHE_TTL);
        } catch (Exception e) {
            // 캐시는 최적화일 뿐이므로 실패해도 요청 처리는 계속한다
            log.warn("event=user_cache_save_failed userId={} reason={}", user.userId(), e.getClass().getSimpleName());
        }
    }

    public Optional<CachedUser> find(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(key(userId));
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, CachedUser.class));
        } catch (Exception e) {
            log.warn("event=user_cache_read_failed userId={} reason={}", userId, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void evict(Long userId) {
        try {
            redisTemplate.delete(key(userId));
        } catch (Exception e) {
            log.warn("event=user_cache_evict_failed userId={} reason={}", userId, e.getClass().getSimpleName());
        }
    }

    private String key(Long userId) {
        return "user_cache:" + userId;
    }
}
