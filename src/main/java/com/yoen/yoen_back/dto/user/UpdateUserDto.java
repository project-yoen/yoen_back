package com.yoen.yoen_back.dto.user;

import com.yoen.yoen_back.enums.Gender;

import java.time.LocalDate;

public record UpdateUserDto(Long userId, String name, Gender gender, String nickname, String birthday) {
}
