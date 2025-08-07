package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Nation;

import java.time.LocalDate;
public record TravelResponseDto(Long travelId, Long numOfPeople, Long numOfJoinedPeople, Nation nation, Long sharedFund, String travelName, LocalDate startDate, LocalDate endDate, String travelImageUrl) {
}
