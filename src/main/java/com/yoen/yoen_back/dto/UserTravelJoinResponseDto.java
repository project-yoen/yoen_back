package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Nation;

import java.util.List;

public record UserTravelJoinResponseDto(Long travelId, String travelName, Nation nation, List<UserResponseDto> users) {
}
