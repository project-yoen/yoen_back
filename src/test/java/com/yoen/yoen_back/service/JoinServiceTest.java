package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidJoinCodeException;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.etc.joincode.AcceptJoinRequestDto;
import com.yoen.yoen_back.dto.etc.joincode.JoinRequestListResponseDto;
import com.yoen.yoen_back.dto.etc.joincode.UserTravelJoinResponseDto;
import com.yoen.yoen_back.entity.image.Image;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinServiceTest {

    // JoinService는 Redis에 저장된 참여 코드와 DB에 저장된 참여 요청을 함께 다룬다.
    // 유닛 테스트에서는 Redis/DB를 실제로 사용하지 않고 DAO와 Repository를 mock으로 대체한다.

    @Mock
    private TravelJoinCodeRedisDao travelJoinCodeRedisDao;

    @Mock
    private TravelJoinRequestRepository travelJoinRequestRepository;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private TravelUserRepository travelUserRepository;

    @Mock
    private TravelService travelService;

    @InjectMocks
    private JoinService joinService;

    // 이미 여행 ID에 대응하는 참여 코드가 Redis에 존재하면 새 코드를 만들면 안 된다.
    // 기존 코드를 그대로 반환하고 saveBidirectionalMapping이 호출되지 않는지 확인한다.
    @Test
    void getJoinCode_existingCode_returnsStoredCode() {
        Travel travel = travel(1L);
        when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));
        when(travelJoinCodeRedisDao.existsTravelId(1L)).thenReturn(true);
        when(travelJoinCodeRedisDao.getCodeByTravelId(1L)).thenReturn(Optional.of("ABC123"));

        String result = joinService.getJoinCode(1L);

        assertThat(result).isEqualTo("ABC123");
        verify(travelJoinCodeRedisDao, never()).saveBidirectionalMapping(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    // Redis에 여행 코드가 없으면 JoinService가 새 6자리 코드를 생성하고 양방향 매핑을 저장해야 한다.
    // SecureRandom 때문에 정확한 코드값은 예측하지 않고 길이와 문자 형식만 검증한다.
    @Test
    void getJoinCode_missingCode_createsAndStoresCode() {
        Travel travel = travel(1L);
        when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));
        when(travelJoinCodeRedisDao.existsTravelId(1L)).thenReturn(false);
        when(travelJoinCodeRedisDao.existsCode(anyString())).thenReturn(false);
        when(travelJoinCodeRedisDao.getCodeByTravelId(1L)).thenReturn(Optional.of("ABC123"));

        String result = joinService.getJoinCode(1L);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(travelJoinCodeRedisDao).saveBidirectionalMapping(codeCaptor.capture(), org.mockito.ArgumentMatchers.eq(1L));
        assertThat(codeCaptor.getValue()).hasSize(6).matches("[A-Z0-9]+");
        assertThat(result).isEqualTo("ABC123");
    }

    // 존재하지 않는 여행에 대해 참여 코드를 발급하면 안 된다.
    // 먼저 TravelRepository에서 여행 존재 여부를 확인하고 없으면 IllegalStateException을 던진다.
    @Test
    void getJoinCode_missingTravel_throwsIllegalStateException() {
        when(travelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinService.getJoinCode(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // Redis에는 travelId key가 있다고 판단됐지만 실제 코드 조회가 실패하는 불일치 상황이다.
    // 서비스는 이 상태를 유효하지 않은 참여 코드 상태로 보고 InvalidJoinCodeException을 던진다.
    @Test
    void getJoinCode_codeLookupFails_throwsInvalidJoinCodeException() {
        Travel travel = travel(1L);
        when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));
        when(travelJoinCodeRedisDao.existsTravelId(1L)).thenReturn(true);
        when(travelJoinCodeRedisDao.getCodeByTravelId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinService.getJoinCode(1L))
                .isInstanceOf(InvalidJoinCodeException.class);
    }

    // 사용자가 입력한 참여 코드가 Redis에서 travelId로 해석되지 않으면 유효하지 않은 코드다.
    // 이 경우 TravelRepository까지 가지 않고 InvalidJoinCodeException으로 중단한다.
    @Test
    void requestToJoinTravel_invalidCode_throwsInvalidJoinCodeException() {
        User user = user(1L);
        when(travelJoinCodeRedisDao.getTravelIdByCode("BAD999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinService.requestToJoinTravel(user, "BAD999"))
                .isInstanceOf(InvalidJoinCodeException.class);
    }

    // 새 신청자는 기존 활성 신청도 없고 이미 참여한 유저도 아닌 상태다.
    // 이때 TravelJoinRequest를 생성해 저장하고, 기본 승인 상태는 false여야 한다.
    @Test
    void requestToJoinTravel_newRequester_savesJoinRequest() {
        User user = user(1L);
        Travel travel = travel(10L);
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of("10"));
        when(travelRepository.getReferenceById(10L)).thenReturn(travel);
        when(travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(List.of());
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of());

        joinService.requestToJoinTravel(user, "ABC123");

        ArgumentCaptor<TravelJoinRequest> requestCaptor = ArgumentCaptor.forClass(TravelJoinRequest.class);
        verify(travelJoinRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTravel()).isEqualTo(travel);
        assertThat(requestCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(requestCaptor.getValue().getIsAccepted()).isFalse();
    }

    // 같은 여행에 이미 활성 참여 신청이 남아 있으면 중복 신청을 저장하면 안 된다.
    // 저장 호출이 일어나지 않는지 verify(never)로 확인한다.
    @Test
    void requestToJoinTravel_existingActiveRequest_doesNotSave() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest existingRequest = joinRequest(100L, travel, user);
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of("10"));
        when(travelRepository.getReferenceById(10L)).thenReturn(travel);
        when(travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(List.of(existingRequest));
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of());

        joinService.requestToJoinTravel(user, "ABC123");

        verify(travelJoinRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // 이미 여행에 참여 중인 유저라면 참여 신청을 새로 만들면 안 된다.
    // 기존 참여자 목록에 같은 userId가 있는 상황을 mock으로 구성한다.
    @Test
    void requestToJoinTravel_alreadyJoinedUser_doesNotSave() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelUser joinedUser = travelUser(20L, travel, user, Role.READER);
        when(travelJoinCodeRedisDao.getTravelIdByCode("ABC123")).thenReturn(Optional.of("10"));
        when(travelRepository.getReferenceById(10L)).thenReturn(travel);
        when(travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(List.of());
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of(joinedUser));

        joinService.requestToJoinTravel(user, "ABC123");

        verify(travelJoinRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // 참여 코드 생성 메서드 자체의 최소 규칙을 확인한다.
    // 랜덤값이므로 특정 문자열 대신 길이와 허용 문자 집합만 검증한다.
    @Test
    void getUniqueJoinCode_returnsSixCharacterAlphaNumericCode() {
        when(travelJoinCodeRedisDao.existsCode(anyString())).thenReturn(false);

        String result = joinService.getUniqueJoinCode(6);

        assertThat(result).hasSize(6).matches("[A-Z0-9]+");
    }

    // 참여 코드의 만료 시간은 Redis TTL을 DAO가 LocalDateTime으로 변환해 제공한다.
    // Service는 DAO 결과를 그대로 반환하는지 확인한다.
    @Test
    void getCodeExpiredTime_existingCode_returnsExpirationTime() {
        LocalDateTime expiredAt = LocalDateTime.of(2025, 7, 1, 12, 0);
        when(travelJoinCodeRedisDao.getExpirationTime("ABC123")).thenReturn(Optional.of(expiredAt));

        LocalDateTime result = joinService.getCodeExpiredTime("ABC123");

        assertThat(result).isEqualTo(expiredAt);
    }

    // 만료 시간 조회가 실패하면 코드가 없거나 TTL이 없는 상태다.
    // 사용자에게는 유효하지 않은 코드로 처리되어야 하므로 InvalidJoinCodeException을 기대한다.
    @Test
    void getCodeExpiredTime_missingCode_throwsInvalidJoinCodeException() {
        when(travelJoinCodeRedisDao.getExpirationTime("BAD999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinService.getCodeExpiredTime("BAD999"))
                .isInstanceOf(InvalidJoinCodeException.class);
    }

    // 여행에 들어온 참여 신청 목록을 화면 응답 DTO로 변환하는 테스트다.
    // 신청자의 프로필 이미지가 있을 때 imageUrl까지 포함되는지 확인한다.
    @Test
    void getJoinRequestList_existingRequests_returnsResponseDtos() {
        User user = user(1L);
        Image image = Image.builder().imageId(5L).objectKey("profile.jpg").imageUrl("https://image.example/profile.jpg").user(user).build();
        user.setProfileImage(image);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        when(travelJoinRequestRepository.findByTravel_TravelIdAndIsActiveTrue(10L)).thenReturn(List.of(request));

        List<JoinRequestListResponseDto> result = joinService.getJoinRequestList(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).travelJoinRequestId()).isEqualTo(100L);
        assertThat(result.get(0).name()).isEqualTo("User 1");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://image.example/profile.jpg");
    }

    // 참여 요청 승인 성공 케이스다.
    // 요청은 accepted=true, isActive=false로 바뀌고, 여행 참여 인원 증가 후 TravelUser가 생성되어야 한다.
    @Test
    void acceptJoinRequest_availableCapacity_acceptsAndCreatesTravelUser() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        when(travelJoinRequestRepository.getReferenceById(100L)).thenReturn(request);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.empty());
        when(travelService.increaseNumOfJoinedPeople(travel)).thenReturn(true);

        joinService.acceptJoinRequest(new AcceptJoinRequestDto(100L, Role.READER));

        assertThat(request.getIsAccepted()).isTrue();
        assertThat(request.getIsActive()).isFalse();
        verify(travelJoinRequestRepository).save(request);
        ArgumentCaptor<TravelUser> travelUserCaptor = ArgumentCaptor.forClass(TravelUser.class);
        verify(travelUserRepository).save(travelUserCaptor.capture());
        assertThat(travelUserCaptor.getValue().getTravel()).isEqualTo(travel);
        assertThat(travelUserCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(travelUserCaptor.getValue().getRole()).isEqualTo(Role.READER);
    }

    // 이미 같은 여행에 참여 중인 유저의 요청을 승인하려 하면 중복 참여가 된다.
    // 이 경우 TravelUser를 새로 저장하지 않고 IllegalStateException을 던져야 한다.
    @Test
    void acceptJoinRequest_alreadyJoined_throwsIllegalStateException() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        TravelUser travelUser = travelUser(20L, travel, user, Role.READER);
        when(travelJoinRequestRepository.getReferenceById(100L)).thenReturn(request);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        assertThatThrownBy(() -> joinService.acceptJoinRequest(new AcceptJoinRequestDto(100L, Role.READER)))
                .isInstanceOf(IllegalStateException.class);
        verify(travelUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // 참여자는 아니지만 여행 정원이 가득 차서 increaseNumOfJoinedPeople이 false를 반환하는 경우다.
    // 정원 초과 시 TravelUser를 만들지 않고 예외로 처리한다.
    @Test
    void acceptJoinRequest_fullCapacity_throwsIllegalStateException() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        when(travelJoinRequestRepository.getReferenceById(100L)).thenReturn(request);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.empty());
        when(travelService.increaseNumOfJoinedPeople(travel)).thenReturn(false);

        assertThatThrownBy(() -> joinService.acceptJoinRequest(new AcceptJoinRequestDto(100L, Role.READER)))
                .isInstanceOf(IllegalStateException.class);
        verify(travelUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // 참여 요청 거절은 승인 여부를 false로 두고 목록에서 보이지 않도록 isActive=false로 soft delete한다.
    // 상태 변경 후 repository save가 호출되는지 확인한다.
    @Test
    void rejectJoinRequest_existingRequest_marksInactiveAndRejected() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        when(travelJoinRequestRepository.getReferenceById(100L)).thenReturn(request);

        joinService.rejectJoinRequest(100L);

        assertThat(request.getIsAccepted()).isFalse();
        assertThat(request.getIsActive()).isFalse();
        verify(travelJoinRequestRepository).save(request);
    }

    // 사용자가 자신이 신청한 여행 목록을 조회하는 흐름이다.
    // 여행 정보와 이미 참여 중인 유저 목록이 UserTravelJoinResponseDto로 조합되는지 확인한다.
    @Test
    void getUserTravelJoinRequests_existingRequests_returnsTravelSummaries() {
        User user = user(1L);
        User joinedUser = user(2L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        TravelUser travelUser = travelUser(20L, travel, joinedUser, Role.WRITER);
        when(travelJoinRequestRepository.findByUserAndIsActiveTrueAndIsAcceptedFalse(user)).thenReturn(List.of(request));
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of(travelUser));

        List<UserTravelJoinResponseDto> result = joinService.getUserTravelJoinRequests(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).travelJoinId()).isEqualTo(100L);
        assertThat(result.get(0).travelId()).isEqualTo(10L);
        assertThat(result.get(0).users()).hasSize(1);
        assertThat(result.get(0).users().get(0).userId()).isEqualTo(2L);
    }

    // 사용자가 본인의 참여 신청을 취소하거나 삭제하는 흐름이다.
    // 실제 삭제가 아니라 isActive=false, isAccepted=false로 soft delete 처리되는지 확인한다.
    @Test
    void deleteUserTravelJoinRequest_existingRequest_marksInactiveAndRejected() {
        User user = user(1L);
        Travel travel = travel(10L);
        TravelJoinRequest request = joinRequest(100L, travel, user);
        when(travelJoinRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        joinService.deleteUserTravelJoinRequest(100L);

        assertThat(request.getIsActive()).isFalse();
        assertThat(request.getIsAccepted()).isFalse();
        verify(travelJoinRequestRepository).save(request);
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .email("user" + userId + "@example.com")
                .password("password")
                .name("User " + userId)
                .nickname("user" + userId)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private Travel travel(Long travelId) {
        return Travel.builder()
                .travelId(travelId)
                .travelName("Tokyo")
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .build();
    }

    private TravelUser travelUser(Long travelUserId, Travel travel, User user, Role role) {
        return TravelUser.builder()
                .travelUserId(travelUserId)
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname("Traveler " + user.getUserId())
                .build();
    }

    private TravelJoinRequest joinRequest(Long requestId, Travel travel, User user) {
        return TravelJoinRequest.builder()
                .travelJoinRequestId(requestId)
                .travel(travel)
                .user(user)
                .isAccepted(false)
                .build();
    }
}
