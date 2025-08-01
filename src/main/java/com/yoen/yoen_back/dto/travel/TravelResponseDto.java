package com.yoen.yoen_back.dto.travel;

import java.time.LocalDate;

public record TravelResponseDto(Long travelId, Long numOfPeople, String travelName, LocalDate startDate, LocalDate endDate,String travelImageUrl) {
}
