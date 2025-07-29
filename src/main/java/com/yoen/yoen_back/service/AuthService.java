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
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.travel.TravelJoinRequestRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final RefreshTokenRedisDao refreshTokenRedisDao;
    private final TravelUserRepository travelUserRepository;
    private final PaymentRepository paymentRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRepository travelRepository;
    private final PaymentImageRepository paymentImageRepository;
    private final TravelRecordImageRepository travelRecordImageRepository;
    private final TravelJoinRequestRepository travelJoinRequestRepository;


    // jwtProvider로 refreshToken 받는 함수
    public String generateRefreshToken(Long userId) {
        return jwtProvider.generateRefreshToken(String.valueOf(userId));
    }

    // jwtProvider로 accessToken 받는 함수
    public String generateAccessToken(Long userId) {
        return jwtProvider.generateAccessToken(String.valueOf(userId));
    }

    // 로그인 기능과 토큰발급을 함께하는 함수
    public LoginResponseDto loginAndGetToken(LoginRequestDto dto) throws InvalidCredentialsException {
        UserResponseDto user = userService.login(dto);
        String userId = String.valueOf(user.userId());

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // redis에 토큰 저장
        refreshTokenRedisDao.save(userId, refreshToken);
        log.info(refreshTokenRedisDao.get(userId));

        return new LoginResponseDto(user, accessToken, refreshToken);
    }

    // accessToken과 refreshToken을 재 발급 하는 함수
    public TokenResponse reissueTokens(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("리프레시 토큰 유효성 검사 실패");
        }
        String userId = jwtProvider.getUserIdFromToken(refreshToken);
        String storedToken = refreshTokenRedisDao.get(userId);

        // 저장되어있는 토큰이 없거나 일치하지 않을시
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidTokenException("리프레시 토큰 불일치 또는 만료됨");
        }
        Long _userId = Long.parseLong(userId);

        String newAccessToken = generateAccessToken(_userId);
        String newRefreshToken = generateRefreshToken(_userId);

        refreshTokenRedisDao.save(userId, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // user와 travelUserId로 일치여부 확인, role과 travelUser role 일치 여부 확인
    public Travel checkTravelUserRoleByTravel(User user, Long travelId, List<Role> roles) {
        Travel tv = travelRepository.findByTravelIdAndIsActiveTrue(travelId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 여행입니다.")) ;;
        return checkTravelUserRole(user, roles, tv);
    }

    // user와 travelUserId로 일치여부 확인, role과 travelUser role 일치 여부 확인
    public Travel checkTravelUserRoleByPayment(User user, Long paymentId, List<Role> roles) {
        Payment pm = paymentRepository.findByPaymentIdAndIsActiveTrue(paymentId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 금액기록입니다.")) ;
        return checkTravelUserRole(user, roles, pm.getTravel());
    }

    public Travel checkTravelUserRoleByRecord(User user, Long recordId, List<Role> roles) {
        TravelRecord tr = travelRecordRepository.findByTravelRecordIdAndIsActiveTrue(recordId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 여행기록입니다.")) ;
        return checkTravelUserRole(user, roles, tr.getTravel());
    }

    public Travel checkTravelUserRoleByPaymentImage(User user, Long paymentImageId, List<Role> roles) {
        PaymentImage pi = paymentImageRepository.findByPaymentImageIdAndIsActiveTrue(paymentImageId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 금액기록-사진입니다.")) ;
        return checkTravelUserRole(user, roles, pi.getPayment().getTravel());
    }

    public Travel checkTravelUserRoleByTravelRecordImage(User user, Long travelRecordImageId, List<Role> roles) {
        TravelRecordImage tri = travelRecordImageRepository.findByTravelRecordImageIdAndIsActiveTrue(travelRecordImageId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 여행기록-사진입니다.")) ;
        return checkTravelUserRole(user, roles, tri.getTravelRecord().getTravel());
    }

    public Travel checkTravelUserRoleByTravelJoinRequest(User user, Long travelJoinRequestId, List<Role> roles) {
        TravelJoinRequest trj = travelJoinRequestRepository.findByTravelJoinRequestIdAndIsActiveTrue(travelJoinRequestId).orElseThrow(() -> new AccessDeniedException("존재하지 않은 승인 요청자입니다."));
        travelUserRepository.findByTravelAndUserAndIsActiveTrue(trj.getTravel(), user).orElseThrow(() -> new AccessDeniedException("승인권한을 가지고 있지 않습니다.")) ;
        return checkTravelUserRole(user, roles, trj.getTravel());
    }

    private Travel checkTravelUserRole(User user, List<Role> roles, Travel travel) {
        TravelUser tu = travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user).orElseThrow(() -> new AccessDeniedException("존재하지 않은 참여자입니다."));
        if (!user.getUserId().equals(tu.getUser().getUserId())) throw new AccessDeniedException("사용자 정보가 일치하지 않습니다.");
        if (!roles.contains(tu.getRole())) throw new AccessDeniedException("접근 권한이 없는 사용자입니다.");
        return tu.getTravel();
    }


    // 로그 아웃시 redis에 저장되어 있는 refreshToken 지우는 함수
    public void logout(String accessToken) {
        String userId = jwtProvider.getUserIdFromToken(accessToken);
        refreshTokenRedisDao.delete(userId);
    }

}
