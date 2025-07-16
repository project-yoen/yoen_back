package com.yoen.yoen_back.dto;

public record TravelRecordRequestDto(Long travelRecordId, Long travelId, Long travelUserId, String title,
                                     String content, String recordTime) {

}
