package com.yoen.yoen_back.dto.etc.joincode;

import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.enums.Nation;

import java.util.List;

public record UserTravelJoinResponseDto(Long travelJoinId, Long travelId, String travelName, Nation nation, List<UserResponseDto> users) {
}
