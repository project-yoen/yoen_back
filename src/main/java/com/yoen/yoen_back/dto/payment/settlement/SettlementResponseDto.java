package com.yoen.yoen_back.dto.payment.settlement;

import com.yoen.yoen_back.dto.travel.TravelUserDto;

import java.util.List;

public record SettlementResponseDto(Long settlementId, Long paymentId, String settlementName, Long amount, Boolean isPaid,
                                   List<TravelUserDto> travelUsers) {
}
