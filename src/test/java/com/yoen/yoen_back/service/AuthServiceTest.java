package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidTokenException;
import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dao.redis.RefreshTokenRedisDao;
import com.yoen.yoen_back.dto.etc.token.TokenResponse;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.LoginResponseDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.travel.TravelJoinRequestRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // AuthService는 JWT, Redis, Repository 등 외부 의존성이 많다.
    // 유닛 테스트에서는 실제 인프라를 띄우지 않고 mock으로 응답을 고정해 AuthService의 분기와 흐름만 검증한다.

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRedisDao refreshTokenRedisDao;

    @Mock
    private TravelUserRepository travelUserRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TravelRecordRepository travelRecordRepository;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private PaymentImageRepository paymentImageRepository;

    @Mock
    private TravelRecordImageRepository travelRecordImageRepository;

    @Mock
    private TravelJoinRequestRepository travelJoinRequestRepository;

    @InjectMocks
    private AuthService authService;

    // access token 생성은 AuthService가 직접 만들지 않고 JwtProvider에 위임한다.
    // 이 테스트는 userId를 문자열로 변환해 provider에 넘기고, provider 결과를 그대로 반환하는지 확인한다.
    @Test
    void generateAccessToken_validUserId_delegatesToJwtProvider() {
        when(jwtProvider.generateAccessToken("1")).thenReturn("access-token");

        String result = authService.generateAccessToken(1L);

        assertThat(result).isEqualTo("access-token");
    }

    // refresh token 생성도 access token과 같은 위임 구조다.
    // 토큰 생성 알고리즘 자체는 JwtProvider 책임이므로 여기서는 위임과 반환값만 검증한다.
    @Test
    void generateRefreshToken_validUserId_delegatesToJwtProvider() {
        when(jwtProvider.generateRefreshToken("1")).thenReturn("refresh-token");

        String result = authService.generateRefreshToken(1L);

        assertThat(result).isEqualTo("refresh-token");
    }

    // 로그인 성공 시 UserService가 인증을 담당하고, AuthService는 토큰 발급과 Redis 저장을 담당한다.
    // 실제 Redis는 사용하지 않고 refreshTokenRedisDao mock에 save가 호출됐는지로 저장 side effect를 검증한다.
    @Test
    void loginAndGetToken_validCredentials_generatesTokensAndStoresRefreshToken() throws Exception {
        LoginRequestDto request = new LoginRequestDto("test@example.com", "password");
        UserResponseDto user = new UserResponseDto(1L, "Test User", "test@example.com", Gender.MALE, "tester", LocalDate.of(2000, 1, 1), "");
        when(userService.login(request)).thenReturn(user);
        when(jwtProvider.generateAccessToken("1")).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("1")).thenReturn("refresh-token");

        LoginResponseDto result = authService.loginAndGetToken(request);

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRedisDao).save("1", "refresh-token");
    }

    // refresh token 자체가 JWT 검증을 통과하지 못하면 Redis 조회도 하지 않고 즉시 예외가 나야 한다.
    // 잘못된 토큰으로 불필요한 저장소 접근이 발생하지 않는지도 함께 확인한다.
    @Test
    void reissueTokens_invalidRefreshToken_throwsInvalidTokenException() {
        when(jwtProvider.validateToken("refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.reissueTokens("refresh-token"))
                .isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRedisDao, never()).get("1");
    }

    // JWT는 유효하지만 Redis에 저장된 토큰이 없는 상황은 만료 또는 로그아웃 상태로 볼 수 있다.
    // 이 경우 새 토큰을 발급하지 않고 InvalidTokenException을 던져야 한다.
    @Test
    void reissueTokens_missingStoredToken_throwsInvalidTokenException() {
        when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn("1");
        when(refreshTokenRedisDao.get("1")).thenReturn(null);

        assertThatThrownBy(() -> authService.reissueTokens("refresh-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // Redis에 저장된 refresh token과 요청으로 들어온 token이 다르면 탈취/재사용 가능성이 있다.
    // 유닛 테스트에서는 Redis mock이 다른 문자열을 반환하게 해서 불일치 분기를 검증한다.
    @Test
    void reissueTokens_mismatchedStoredToken_throwsInvalidTokenException() {
        when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn("1");
        when(refreshTokenRedisDao.get("1")).thenReturn("other-refresh-token");

        assertThatThrownBy(() -> authService.reissueTokens("refresh-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // refresh token이 유효하고 Redis 저장값과도 일치하면 access/refresh token을 새로 발급한다.
    // 새 refresh token은 Redis에 다시 저장되어야 하므로 반환 DTO와 save 호출을 함께 검증한다.
    @Test
    void reissueTokens_validRefreshToken_returnsNewTokensAndUpdatesRedis() {
        when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn("1");
        when(refreshTokenRedisDao.get("1")).thenReturn("refresh-token");
        when(jwtProvider.generateAccessToken("1")).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken("1")).thenReturn("new-refresh-token");

        TokenResponse result = authService.reissueTokens("refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRedisDao).save("1", "new-refresh-token");
    }

    // 로그아웃은 access token에서 userId를 꺼낸 뒤 Redis의 refresh token을 삭제하는 흐름이다.
    // 실제 Redis 삭제가 아니라 DAO mock의 delete 호출 여부를 검증한다.
    @Test
    void logout_validAccessToken_deletesRefreshToken() {
        when(jwtProvider.getUserIdFromToken("access-token")).thenReturn("1");

        authService.logout("access-token");

        verify(refreshTokenRedisDao).delete("1");
    }

    // 여행 권한 체크의 정상 케이스다.
    // 여행이 존재하고, 해당 유저가 여행 참여자이며, 역할이 허용 목록에 포함되면 TravelUser를 반환해야 한다.
    @Test
    void checkTravelUserRoleByTravel_allowedRole_returnsTravelUser() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        when(travelRepository.findByTravelIdAndIsActiveTrue(1L)).thenReturn(Optional.of(travel));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByTravel(user, 1L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
    }

    // 여행 ID로 활성 여행을 찾지 못하면 이후 권한 체크를 진행할 수 없다.
    // 서비스는 존재하지 않는 여행을 AccessDeniedException으로 막는다.
    @Test
    void checkTravelUserRoleByTravel_missingTravel_throwsAccessDeniedException() {
        User user = user(1L);
        when(travelRepository.findByTravelIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.checkTravelUserRoleByTravel(user, 1L, List.of(Role.WRITER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // 여행은 존재하지만 요청 유저가 해당 여행의 활성 참여자가 아닌 경우다.
    // 이 상태에서는 여행 정보를 보거나 수정할 권한이 없으므로 예외가 발생해야 한다.
    @Test
    void checkTravelUserRoleByTravel_missingTravelUser_throwsAccessDeniedException() {
        User user = user(1L);
        Travel travel = travel(1L);
        when(travelRepository.findByTravelIdAndIsActiveTrue(1L)).thenReturn(Optional.of(travel));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.checkTravelUserRoleByTravel(user, 1L, List.of(Role.WRITER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // Repository가 반환한 TravelUser의 userId가 요청 유저와 다르면 데이터가 일치하지 않는 상태다.
    // 방어 로직으로 사용자 불일치를 감지해 AccessDeniedException을 던지는지 확인한다.
    @Test
    void checkTravelUserRoleByTravel_userMismatch_throwsAccessDeniedException() {
        User requestUser = user(1L);
        User savedUser = user(2L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, savedUser, Role.WRITER);
        when(travelRepository.findByTravelIdAndIsActiveTrue(1L)).thenReturn(Optional.of(travel));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, requestUser)).thenReturn(Optional.of(travelUser));

        assertThatThrownBy(() -> authService.checkTravelUserRoleByTravel(requestUser, 1L, List.of(Role.WRITER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // 참여자는 맞지만 역할이 허용 목록에 포함되지 않은 경우다.
    // 예를 들어 WRITER 권한이 필요한 작업에 READER가 접근하면 차단되어야 한다.
    @Test
    void checkTravelUserRoleByTravel_disallowedRole_throwsAccessDeniedException() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.READER);
        when(travelRepository.findByTravelIdAndIsActiveTrue(1L)).thenReturn(Optional.of(travel));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        assertThatThrownBy(() -> authService.checkTravelUserRoleByTravel(user, 1L, List.of(Role.WRITER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // 결제 권한 체크는 paymentId로 Payment를 찾고, Payment가 속한 Travel을 기준으로 공통 권한 로직을 탄다.
    // 여기서는 Payment -> Travel 경로가 올바르게 사용되는지 대표 케이스로 확인한다.
    @Test
    void checkTravelUserRoleByPayment_existingPayment_usesPaymentTravel() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        Payment payment = Payment.builder().paymentId(100L).travel(travel).build();
        when(paymentRepository.findByPaymentIdAndIsActiveTrue(100L)).thenReturn(Optional.of(payment));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByPayment(user, 100L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
    }

    // 여행 기록 권한 체크는 recordId로 TravelRecord를 찾고, 기록이 속한 Travel을 기준으로 권한을 판단한다.
    // RecordService의 수정/삭제 전에 호출되는 권한 흐름을 검증하는 대표 케이스다.
    @Test
    void checkTravelUserRoleByRecord_existingRecord_usesRecordTravel() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = TravelRecord.builder().travelRecordId(100L).travel(travel).build();
        when(travelRecordRepository.findByTravelRecordIdAndIsActiveTrue(100L)).thenReturn(Optional.of(record));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByRecord(user, 100L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
    }

    // 결제 이미지 권한 체크는 PaymentImage -> Payment -> Travel 순서로 여행을 찾아간다.
    // 이미지 삭제 같은 작업에서 해당 여행의 작성자 권한을 확인할 수 있어야 한다.
    @Test
    void checkTravelUserRoleByPaymentImage_existingImage_usesPaymentTravel() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        Payment payment = Payment.builder().paymentId(100L).travel(travel).build();
        PaymentImage paymentImage = PaymentImage.builder().paymentImageId(200L).payment(payment).build();
        when(paymentImageRepository.findByPaymentImageIdAndIsActiveTrue(200L)).thenReturn(Optional.of(paymentImage));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByPaymentImage(user, 200L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
    }

    // 여행 기록 이미지 권한 체크는 TravelRecordImage -> TravelRecord -> Travel 경로를 사용한다.
    // 이미지가 직접 여행을 들고 있지 않으므로 중간 엔티티 연결을 제대로 따라가는지 확인한다.
    @Test
    void checkTravelUserRoleByTravelRecordImage_existingImage_usesRecordTravel() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = TravelRecord.builder().travelRecordId(100L).travel(travel).build();
        TravelRecordImage image = TravelRecordImage.builder().travelRecordImageId(200L).travelRecord(record).build();
        when(travelRecordImageRepository.findByTravelRecordImageIdAndIsActiveTrue(200L)).thenReturn(Optional.of(image));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByTravelRecordImage(user, 200L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
    }

    // 참여 요청 승인 권한은 TravelJoinRequest가 속한 여행을 기준으로 판단한다.
    // 요청자가 해당 여행의 참여자인지 확인한 뒤 공통 역할 검증까지 통과해야 한다.
    @Test
    void checkTravelUserRoleByTravelJoinRequest_existingRequest_usesRequestTravel() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelJoinRequest request = TravelJoinRequest.builder().travelJoinRequestId(100L).travel(travel).user(user).build();
        when(travelJoinRequestRepository.findByTravelJoinRequestIdAndIsActiveTrue(100L)).thenReturn(Optional.of(request));
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));

        TravelUser result = authService.checkTravelUserRoleByTravelJoinRequest(user, 100L, List.of(Role.WRITER));

        assertThat(result).isEqualTo(travelUser);
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
                .travelNickname("traveler")
                .build();
    }
}
