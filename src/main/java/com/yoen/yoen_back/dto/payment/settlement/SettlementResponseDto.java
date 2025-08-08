package com.yoen.yoen_back.dto.payment.settlement;

import com.yoen.yoen_back.dto.travel.TravelUserResponseDto;

import java.util.List;

public record SettlementResponseDto(Long settlementId, Long paymentId, String settlementName, Long amount, Boolean isPaid,
                                    List<TravelUserResponseDto> travelUsers) {
}
