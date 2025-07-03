package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.user.User;

public record LoginResponseDto(User user, String accessToken, String refreshToken) {
}
