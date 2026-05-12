package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TravelRecordRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private TravelRecordRepository travelRecordRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private TravelUserRepository travelUserRepository;

    @Autowired
    private UserRepository userRepository;

    // 같은 여행에 속한 기록 중 active 상태인 기록만 조회되는지 검증한다.
    // PostgreSQL에 실제로 active/inactive 데이터를 저장한 뒤 Repository query method 결과를 확인한다.
    @Test
    void findByTravelAndIsActiveTrue_existingActiveRecords_returnsOnlyActiveRecords() {
        Travel travel = saveTravel("Tokyo");
        TravelUser travelUser = saveTravelUser(travel, saveUser("writer"));
        TravelRecord activeRecord = saveTravelRecord(travel, travelUser, "active", LocalDateTime.of(2025, 7, 1, 10, 0), true);
        saveTravelRecord(travel, travelUser, "inactive", LocalDateTime.of(2025, 7, 1, 11, 0), false);

        List<TravelRecord> result = travelRecordRepository.findByTravelAndIsActiveTrue(travel);

        assertThat(result)
                .extracting(TravelRecord::getTravelRecordId)
                .containsExactly(activeRecord.getTravelRecordId());
    }

    // travelId 기준 조회가 대상 여행의 active 기록만 반환하는지 검증한다.
    // 다른 여행에 active 기록이 있어도 결과에 섞이면 안 된다.
    @Test
    void findByTravel_TravelIdAndIsActiveTrue_existingActiveRecords_returnsOnlyMatchingTravelRecords() {
        Travel targetTravel = saveTravel("Tokyo");
        Travel otherTravel = saveTravel("Osaka");
        TravelUser targetTravelUser = saveTravelUser(targetTravel, saveUser("target"));
        TravelUser otherTravelUser = saveTravelUser(otherTravel, saveUser("other"));
        TravelRecord targetRecord = saveTravelRecord(targetTravel, targetTravelUser, "target", LocalDateTime.of(2025, 7, 1, 10, 0), true);
        saveTravelRecord(otherTravel, otherTravelUser, "other", LocalDateTime.of(2025, 7, 1, 10, 0), true);

        List<TravelRecord> result = travelRecordRepository.findByTravel_TravelIdAndIsActiveTrue(targetTravel.getTravelId());

        assertThat(result)
                .extracting(TravelRecord::getTravelRecordId)
                .containsExactly(targetRecord.getTravelRecordId());
    }

    // active 여행 기록은 ID 기준 조회에서 Optional로 반환되는지 검증한다.
    // soft delete 조건인 isActive=true가 query method에 정상 반영되는지 보는 기본 케이스다.
    @Test
    void findByTravelRecordIdAndIsActiveTrue_activeRecord_returnsRecord() {
        Travel travel = saveTravel("Tokyo");
        TravelUser travelUser = saveTravelUser(travel, saveUser("writer"));
        TravelRecord activeRecord = saveTravelRecord(travel, travelUser, "active", LocalDateTime.of(2025, 7, 1, 10, 0), true);

        Optional<TravelRecord> result = travelRecordRepository.findByTravelRecordIdAndIsActiveTrue(activeRecord.getTravelRecordId());

        assertThat(result).isPresent();
        assertThat(result.get().getTravelRecordId()).isEqualTo(activeRecord.getTravelRecordId());
    }

    // ID가 맞더라도 inactive 여행 기록은 조회되지 않아야 한다.
    // 실제 PostgreSQL row는 남아 있지만 Repository의 active 조건이 필터링하는지 확인한다.
    @Test
    void findByTravelRecordIdAndIsActiveTrue_inactiveRecord_returnsEmpty() {
        Travel travel = saveTravel("Tokyo");
        TravelUser travelUser = saveTravelUser(travel, saveUser("writer"));
        TravelRecord inactiveRecord = saveTravelRecord(travel, travelUser, "inactive", LocalDateTime.of(2025, 7, 1, 10, 0), false);

        Optional<TravelRecord> result = travelRecordRepository.findByTravelRecordIdAndIsActiveTrue(inactiveRecord.getTravelRecordId());

        assertThat(result).isEmpty();
    }

    // 날짜 범위 조회가 대상 여행, active 상태, recordTime 조건을 함께 만족하는 기록만 반환하는지 검증한다.
    // 범위 밖 기록, 다른 여행 기록, inactive 기록을 함께 넣어 필터 조건을 분리해서 확인한다.
    @Test
    void findAllByTravelAndRecordTimeBetweenAndIsActiveTrue_recordsAcrossDates_returnsRecordsInsideRange() {
        Travel targetTravel = saveTravel("Tokyo");
        Travel otherTravel = saveTravel("Osaka");
        TravelUser targetTravelUser = saveTravelUser(targetTravel, saveUser("target"));
        TravelUser otherTravelUser = saveTravelUser(otherTravel, saveUser("other"));
        LocalDateTime start = LocalDateTime.of(2025, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 7, 2, 0, 0);
        saveTravelRecord(targetTravel, targetTravelUser, "before", start.minusSeconds(1), true);
        TravelRecord insideRecord = saveTravelRecord(targetTravel, targetTravelUser, "inside", start.plusHours(10), true);
        saveTravelRecord(targetTravel, targetTravelUser, "inactive", start.plusHours(11), false);
        saveTravelRecord(otherTravel, otherTravelUser, "other", start.plusHours(10), true);
        saveTravelRecord(targetTravel, targetTravelUser, "after", end.plusSeconds(1), true);

        List<TravelRecord> result = travelRecordRepository.findAllByTravelAndRecordTimeBetweenAndIsActiveTrue(targetTravel, start, end);

        assertThat(result)
                .extracting(TravelRecord::getTravelRecordId)
                .containsExactly(insideRecord.getTravelRecordId());
    }

    // 현재 Repository의 Between query가 시작/끝 경계를 모두 포함하는 동작을 문서화한다.
    // 하루 조회를 반열린 구간으로 원한다면 이 테스트가 변경 필요 지점을 드러낸다.
    @Test
    void findAllByTravelAndRecordTimeBetweenAndIsActiveTrue_recordAtBoundary_documentsCurrentBetweenBehavior() {
        Travel travel = saveTravel("Tokyo");
        TravelUser travelUser = saveTravelUser(travel, saveUser("writer"));
        LocalDateTime start = LocalDateTime.of(2025, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 7, 2, 0, 0);
        TravelRecord startBoundaryRecord = saveTravelRecord(travel, travelUser, "start", start, true);
        TravelRecord endBoundaryRecord = saveTravelRecord(travel, travelUser, "end", end, true);

        List<TravelRecord> result = travelRecordRepository.findAllByTravelAndRecordTimeBetweenAndIsActiveTrue(travel, start, end);

        assertThat(result)
                .extracting(TravelRecord::getTravelRecordId)
                .containsExactlyInAnyOrder(startBoundaryRecord.getTravelRecordId(), endBoundaryRecord.getTravelRecordId());
    }

    private User saveUser(String suffix) {
        User user = User.builder()
                .email(suffix + "@example.com")
                .password("password")
                .name("User " + suffix)
                .nickname("user-" + suffix)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Travel saveTravel(String travelName) {
        Travel travel = Travel.builder()
                .travelName(travelName)
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
        return travelRepository.saveAndFlush(travel);
    }

    private TravelUser saveTravelUser(Travel travel, User user) {
        TravelUser travelUser = TravelUser.builder()
                .travel(travel)
                .user(user)
                .role(Role.WRITER)
                .travelNickname(user.getNickname())
                .build();
        return travelUserRepository.saveAndFlush(travelUser);
    }

    private TravelRecord saveTravelRecord(Travel travel, TravelUser travelUser, String title, LocalDateTime recordTime, boolean active) {
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(travel)
                .travelUser(travelUser)
                .title(title)
                .content("content")
                .recordTime(recordTime)
                .build();
        travelRecord.setIsActive(active);
        return travelRecordRepository.saveAndFlush(travelRecord);
    }
}
