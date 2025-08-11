package com.yoen.yoen_back.dto.payment.settlement;

import java.util.List;

public record SettlementRequestDto(Long settlementId, Long paymentId, String settlementName, Long amount, Boolean isPaid,
                                   List<SettlementParticipantDto> travelUsers) {
}
