package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.travel.TravelRecord;

import java.time.LocalDateTime;
import java.util.List;

public record TravelRecordResponseDto(Long travelRecordId, String title, String content, LocalDateTime recordTime, List<TravelRecordImageDto> images) {
}
