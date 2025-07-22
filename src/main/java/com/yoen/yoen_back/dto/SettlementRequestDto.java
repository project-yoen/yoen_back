package com.yoen.yoen_back.dto;

import java.util.List;

public record SettlementRequestDto(Long settlementId, Long paymentId, String settlementName, Long amount, Boolean isPaid,
                                   List<Long> travelUsers) {
}
