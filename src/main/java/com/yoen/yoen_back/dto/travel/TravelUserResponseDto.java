package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Gender;

import java.time.LocalDate;

public record TravelUserResponseDto(Long travelUserId, String nickName, String travelNickname, Gender gender, LocalDate birthDay, String imageUrl) {
}
