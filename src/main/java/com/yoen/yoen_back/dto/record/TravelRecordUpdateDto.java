package com.yoen.yoen_back.dto.record;

import java.util.List;

public record TravelRecordUpdateDto(Long travelRecordId, Long travelId, String title, String content, String recordTime, List<Long> removeImageIds) {
}
