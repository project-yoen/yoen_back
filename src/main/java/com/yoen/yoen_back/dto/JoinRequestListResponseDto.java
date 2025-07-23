package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.image.Image;

public record JoinRequestListResponseDto(Long travelJoinRequestId, Long userId, String name, Boolean isAccepted, Image profileImage) {
}
