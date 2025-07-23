package com.yoen.yoen_back.dto.etc.joincode;

import java.time.LocalDateTime;

public record JoinCodeResponseDto(String code, LocalDateTime expiredAt) {
    public static JoinCodeResponseDto joinCode(String code, LocalDateTime expiredAt) {
        return new JoinCodeResponseDto(code, expiredAt);
    }
}
