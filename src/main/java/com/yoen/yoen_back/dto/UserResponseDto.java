package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Gender;

import java.time.LocalDate;

public record UserResponseDto(Long userId, String name, String email, Gender gender,String nickname, LocalDate birthday, String imageUrl) {
}
