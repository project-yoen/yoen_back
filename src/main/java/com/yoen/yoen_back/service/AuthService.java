package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidTokenException;
import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dao.redis.RefreshTokenRedisDao;
import com.yoen.yoen_back.dto.etc.token.TokenResponse;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.LoginResponseDto;
import com.yoen.yoen_back.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final RefreshTokenRedisDao refreshTokenRedisDao;


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
        User user = userService.login(dto);
        String userId = String.valueOf(user.getUserId());

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

    // 로그 아웃시 redis에 저장되어 있는 refreshToken 지우는 함수
    public void logout(String accessToken) {
        String userId = jwtProvider.getUserIdFromToken(accessToken);
        refreshTokenRedisDao.delete(userId);
    }

}
