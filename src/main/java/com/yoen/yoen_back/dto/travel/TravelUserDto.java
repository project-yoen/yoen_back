package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Role;

public record TravelUserDto(Long travelUserId, Long travelId, Long userId, Role role, String travelNickname) {
}
