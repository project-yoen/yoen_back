package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Gender;

import java.time.LocalDate;

public record TravelUserResponseDto(String nickName, String travelNickName, Gender gender, LocalDate birthDay, String imageUrl) {
}
