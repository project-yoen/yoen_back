package com.yoen.yoen_back.dto.record;

public record TravelRecordRequestDto(Long travelRecordId, Long travelId, String title,
                                     String content, String recordTime) {

}
