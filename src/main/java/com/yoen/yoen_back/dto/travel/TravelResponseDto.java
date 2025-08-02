package com.yoen.yoen_back.dto.travel;

import java.time.LocalDate;
// Todo: 여기도 현재 인원수 추가하고 프론트에서 처리해야함
public record TravelResponseDto(Long travelId, Long numOfPeople, String travelName, LocalDate startDate, LocalDate endDate,String travelImageUrl) {
}
