package com.yoen.yoen_back.dto.payment.settlement;

public record SettlementUserResponseDto(Long settlementUserId, Long settlementId, Long travelUserId, Long amount,
                                        Boolean isPaid) {
}
