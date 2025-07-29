package com.yoen.yoen_back.dto.user;

public record LoginResponseDto(UserResponseDto user, String accessToken, String refreshToken) {
}
