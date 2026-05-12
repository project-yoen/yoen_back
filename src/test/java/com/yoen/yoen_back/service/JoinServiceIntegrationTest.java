package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidJoinCodeException;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.etc.joincode.AcceptJoinRequestDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.travel.TravelJoinRequestRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JoinService.class, TravelService.class})
class JoinServiceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JoinService joinService;

    @Autowired
    private TravelJoinRequestRepository travelJoinRequestRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private TravelUserRepository travelUserRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private TravelJoinCodeRedisDao travelJoinCodeRedisDao;

    @MockitoBean
    private CommonService commonService;

    @MockitoBean
    private ImageService imageService;

    // Redis mock이 유효한 참여 코드를 travelId로 변환해주면 참여 신청이 PostgreSQL에 저장되어야 한다.
    // 저장된 요청의 기본 상태는 승인 전이므로 isAccepted=false, isActive=true여야 한다.
    @Test
    void requestToJoinTravel_validCode_savesJoinRequest() {
        Travel travel = saveTravel("Tokyo", 3L, 1L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User requester = saveUser("requester");
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of(travel.getTravelId().toString()));

        joinService.requestToJoinTravel(requester, "ABC123");

        List<TravelJoinRequest> result = travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, requester);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsAccepted()).isFalse();
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    // 같은 사용자와 같은 여행에 active 참여 신청이 이미 있으면 중복 신청이 저장되면 안 된다.
    // 실제 DB row 개수가 1개로 유지되는지 확인한다.
    @Test
    void requestToJoinTravel_existingActiveRequest_doesNotCreateDuplicateRequest() {
        Travel travel = saveTravel("Tokyo", 3L, 1L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User requester = saveUser("requester");
        saveJoinRequest(travel, requester, false, true);
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of(travel.getTravelId().toString()));

        joinService.requestToJoinTravel(requester, "ABC123");

        List<TravelJoinRequest> result = travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, requester);
        assertThat(result).hasSize(1);
    }

    // 이미 여행에 참여 중인 사용자는 참여 신청을 다시 만들 수 없어야 한다.
    // TravelUser가 존재하는 상태에서 JoinRequest가 생성되지 않는지 검증한다.
    @Test
    void requestToJoinTravel_alreadyJoinedUser_doesNotCreateRequest() {
        Travel travel = saveTravel("Tokyo", 3L, 2L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User joinedUser = saveUser("joined");
        saveTravelUser(travel, joinedUser, Role.READER);
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of(travel.getTravelId().toString()));

        joinService.requestToJoinTravel(joinedUser, "ABC123");

        List<TravelJoinRequest> result = travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, joinedUser);
        assertThat(result).isEmpty();
    }

    // Redis에서 참여 코드를 travelId로 찾지 못하면 유효하지 않은 코드로 처리해야 한다.
    // 예외 발생 후 PostgreSQL에 참여 신청이 저장되지 않았는지도 함께 확인한다.
    @Test
    void requestToJoinTravel_invalidCode_throwsInvalidJoinCodeException() {
        User requester = saveUser("requester");
        when(travelJoinCodeRedisDao.getTravelIdByCode("BAD999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinService.requestToJoinTravel(requester, "BAD999"))
                .isInstanceOf(InvalidJoinCodeException.class);
        assertThat(travelJoinRequestRepository.count()).isZero();
    }

    // 정원이 남아 있는 참여 요청을 승인하면 요청은 비활성화되고 TravelUser가 새로 생성되어야 한다.
    // 동시에 Travel.numOfJoinedPeople도 실제 DB에서 1 증가하는지 확인한다.
    @Test
    void acceptJoinRequest_availableCapacity_acceptsRequestAndCreatesTravelUser() {
        Travel travel = saveTravel("Tokyo", 3L, 1L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User requester = saveUser("requester");
        TravelJoinRequest request = saveJoinRequest(travel, requester, false, true);

        joinService.acceptJoinRequest(new AcceptJoinRequestDto(request.getTravelJoinRequestId(), Role.READER));

        TravelJoinRequest savedRequest = travelJoinRequestRepository.findById(request.getTravelJoinRequestId()).orElseThrow();
        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        Optional<TravelUser> savedTravelUser = travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, requester);
        assertThat(savedRequest.getIsAccepted()).isTrue();
        assertThat(savedRequest.getIsActive()).isFalse();
        assertThat(savedTravelUser).isPresent();
        assertThat(savedTravelUser.get().getRole()).isEqualTo(Role.READER);
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(2L);
    }

    // 이미 참여 중인 사용자의 요청을 승인하려 하면 중복 TravelUser가 생성되면 안 된다.
    // 현재 서비스는 예외를 던지며, 이 테스트는 DB에 참여자 row가 추가되지 않는지를 검증한다.
    @Test
    void acceptJoinRequest_alreadyJoined_throwsIllegalStateExceptionAndDoesNotCreateDuplicateTravelUser() {
        Travel travel = saveTravel("Tokyo", 3L, 2L);
        User writer = saveUser("writer");
        saveTravelUser(travel, writer, Role.WRITER);
        User joinedUser = saveUser("joined");
        saveTravelUser(travel, joinedUser, Role.READER);
        TravelJoinRequest request = saveJoinRequest(travel, joinedUser, false, true);

        assertThatThrownBy(() -> joinService.acceptJoinRequest(new AcceptJoinRequestDto(request.getTravelJoinRequestId(), Role.READER)))
                .isInstanceOf(IllegalStateException.class);

        List<TravelUser> travelUsers = travelUserRepository.findByTravelAndIsActiveTrue(travel);
        assertThat(travelUsers)
                .extracting(tu -> tu.getUser().getUserId())
                .containsExactlyInAnyOrder(writer.getUserId(), joinedUser.getUserId());
        assertThat(travelUsers).hasSize(2);
    }

    // 여행 정원이 가득 찬 상태에서는 승인 요청이 실패하고 TravelUser가 생성되면 안 된다.
    // 참여 인원 수가 증가하지 않고 기존 값 그대로 유지되는지도 확인한다.
    @Test
    void acceptJoinRequest_fullCapacity_throwsIllegalStateExceptionAndDoesNotCreateTravelUser() {
        Travel travel = saveTravel("Tokyo", 1L, 1L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User requester = saveUser("requester");
        TravelJoinRequest request = saveJoinRequest(travel, requester, false, true);

        assertThatThrownBy(() -> joinService.acceptJoinRequest(new AcceptJoinRequestDto(request.getTravelJoinRequestId(), Role.READER)))
                .isInstanceOf(IllegalStateException.class);

        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        Optional<TravelUser> savedTravelUser = travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, requester);
        assertThat(savedTravelUser).isEmpty();
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(1L);
    }

    // 참여 요청을 거절하면 isAccepted=false, isActive=false로 soft delete 상태가 되어야 한다.
    // 거절된 요청이 active 조회 결과에서 제외되는지도 확인한다.
    @Test
    void rejectJoinRequest_existingRequest_marksInactiveAndRejected() {
        Travel travel = saveTravel("Tokyo", 3L, 1L);
        saveTravelUser(travel, saveUser("writer"), Role.WRITER);
        User requester = saveUser("requester");
        TravelJoinRequest request = saveJoinRequest(travel, requester, true, true);

        joinService.rejectJoinRequest(request.getTravelJoinRequestId());

        TravelJoinRequest savedRequest = travelJoinRequestRepository.findById(request.getTravelJoinRequestId()).orElseThrow();
        List<TravelJoinRequest> activeRequests = travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, requester);
        assertThat(savedRequest.getIsAccepted()).isFalse();
        assertThat(savedRequest.getIsActive()).isFalse();
        assertThat(activeRequests).isEmpty();
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

    private Travel saveTravel(String travelName, Long numOfPeople, Long numOfJoinedPeople) {
        Travel travel = Travel.builder()
                .travelName(travelName)
                .numOfPeople(numOfPeople)
                .numOfJoinedPeople(numOfJoinedPeople)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
        return travelRepository.saveAndFlush(travel);
    }

    private TravelUser saveTravelUser(Travel travel, User user, Role role) {
        TravelUser travelUser = TravelUser.builder()
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname(user.getNickname())
                .build();
        return travelUserRepository.saveAndFlush(travelUser);
    }

    private TravelJoinRequest saveJoinRequest(Travel travel, User user, boolean accepted, boolean active) {
        TravelJoinRequest request = TravelJoinRequest.builder()
                .travel(travel)
                .user(user)
                .isAccepted(accepted)
                .build();
        request.setIsActive(active);
        return travelJoinRequestRepository.saveAndFlush(request);
    }
}
