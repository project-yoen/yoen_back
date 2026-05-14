package com.yoen.yoen_back.dao.redis;

import com.yoen.yoen_back.support.RedisIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        TravelJoinCodeRedisDao.class,
        RedisIntegrationTestSupport.RedisTestConfig.class
})
class TravelJoinCodeRedisDaoIntegrationTest extends RedisIntegrationTestSupport {

    @Autowired
    private TravelJoinCodeRedisDao travelJoinCodeRedisDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void flushRedis() {
        try (RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    // 참여 코드를 저장하면 code -> travelId, travelId -> code 양방향 매핑이 모두 Redis에 남아야 한다.
    // 실제 Redis 컨테이너에 저장한 뒤 DAO 조회 메서드로 양쪽 방향을 확인한다.
    @Test
    void saveBidirectionalMapping_validCodeAndTravelId_savesBothDirections() {
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        Optional<String> travelId = travelJoinCodeRedisDao.getTravelIdByCode("ABC123");
        Optional<String> code = travelJoinCodeRedisDao.getCodeByTravelId(1L);

        assertThat(travelId).contains("1");
        assertThat(code).contains("ABC123");
    }

    // 저장된 참여 코드 key가 Redis에 존재하는지 existsCode가 실제 Redis 상태를 기준으로 반환하는지 검증한다.
    @Test
    void existsCode_existingCode_returnsTrue() {
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        boolean result = travelJoinCodeRedisDao.existsCode("ABC123");

        assertThat(result).isTrue();
    }

    // 저장된 여행 ID key가 Redis에 존재하는지 existsTravelId가 실제 Redis 상태를 기준으로 반환하는지 검증한다.
    @Test
    void existsTravelId_existingTravelId_returnsTrue() {
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        boolean result = travelJoinCodeRedisDao.existsTravelId(1L);

        assertThat(result).isTrue();
    }

    // code 기준 삭제를 수행하면 joinCode key뿐 아니라 반대 방향 travelCode key도 함께 삭제되어야 한다.
    // 양방향 매핑 중 한쪽만 남는 불일치 상태를 방지하기 위한 테스트다.
    @Test
    void deleteByCode_existingCode_deletesBothDirections() {
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        travelJoinCodeRedisDao.deleteByCode("ABC123");

        assertThat(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).isEmpty();
        assertThat(travelJoinCodeRedisDao.getCodeByTravelId(1L)).isEmpty();
    }

    // travelId 기준 삭제를 수행하면 travelCode key뿐 아니라 반대 방향 joinCode key도 함께 삭제되어야 한다.
    // 여행 삭제나 코드 폐기 시 Redis 양방향 매핑이 깨끗하게 정리되는지 확인한다.
    @Test
    void deleteByTravelId_existingTravelId_deletesBothDirections() {
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        travelJoinCodeRedisDao.deleteByTravelId(1L);

        assertThat(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).isEmpty();
        assertThat(travelJoinCodeRedisDao.getCodeByTravelId(1L)).isEmpty();
    }

    // 저장된 참여 코드에는 TTL이 설정되어야 하고, 만료 시간은 현재보다 미래여야 한다.
    // 정확한 초 단위 비교는 flaky할 수 있으므로 3일 TTL 주변의 느슨한 범위로 검증한다.
    @Test
    void getExpirationTime_existingCode_returnsFutureTime() {
        LocalDateTime beforeLookup = LocalDateTime.now();
        travelJoinCodeRedisDao.saveBidirectionalMapping("ABC123", 1L);

        Optional<LocalDateTime> result = travelJoinCodeRedisDao.getExpirationTime("ABC123");

        assertThat(result).isPresent();
        assertThat(result.get()).isAfter(beforeLookup.plusDays(2));
        assertThat(result.get()).isBefore(LocalDateTime.now().plusDays(3).plusMinutes(1));
    }

    // Redis에 없는 참여 코드의 만료 시간을 조회하면 empty를 반환해야 한다.
    // 없는 key를 유효한 코드처럼 처리하지 않는지 확인한다.
    @Test
    void getExpirationTime_missingCode_returnsEmpty() {
        Optional<LocalDateTime> result = travelJoinCodeRedisDao.getExpirationTime("MISSING");

        assertThat(result).isEmpty();
    }
}
