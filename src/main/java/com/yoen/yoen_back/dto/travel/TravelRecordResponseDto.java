package com.yoen.yoen_back.dto.travel;

import java.time.LocalDateTime;
import java.util.List;

public record TravelRecordResponseDto(Long travelRecordId, String title, String content, LocalDateTime recordTime, List<TravelRecordImageDto> images) {
}
