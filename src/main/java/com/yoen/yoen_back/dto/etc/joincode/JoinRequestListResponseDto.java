package com.yoen.yoen_back.dto.etc.joincode;

public record JoinRequestListResponseDto(Long travelJoinRequestId, com.yoen.yoen_back.enums.Gender gender, String name, String imageUrl) {
}
