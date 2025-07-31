package com.yoen.yoen_back.dto.travel;

public record TravelRecordRequestDto(Long travelRecordId, Long travelId, String title,
                                     String content, String recordTime) {

}
