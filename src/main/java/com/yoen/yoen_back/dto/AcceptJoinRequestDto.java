package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Role;

public record AcceptJoinRequestDto(Long travelJoinRequestId, Role role) {
}
