package com.yoen.yoen_back.dto.travel;

import com.yoen.yoen_back.enums.Nation;

import java.util.List;

public record TravelRequestDto (Long travelId, String travelName, Long numOfPeople, Nation nation, String startDate, String endDate, List<Long> destinationIds) {
}
