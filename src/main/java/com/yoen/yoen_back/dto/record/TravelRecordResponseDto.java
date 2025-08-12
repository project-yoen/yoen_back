package com.yoen.yoen_back.dto.record;

import java.time.LocalDateTime;
import java.util.List;

public record TravelRecordResponseDto(Long travelRecordId, String travelNickName, String title, String content, LocalDateTime recordTime, List<TravelRecordImageDto> images) {
}
