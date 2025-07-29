package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Nation;

public record DestinationResponseDto(Long destinationId, Nation nation, String destinationName) {
}
