package com.yoen.yoen_back.dto.travel;

import java.time.LocalDate;

public record TravelResponseDto(Long travelId, String travelName, LocalDate startDate, String imageUrl) {
}
