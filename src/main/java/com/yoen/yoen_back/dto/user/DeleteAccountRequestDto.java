package com.yoen.yoen_back.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequestDto(
        @NotBlank(message = "비밀번호를 입력해주세요.") String password
) {
}
