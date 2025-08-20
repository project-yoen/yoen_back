package com.yoen.yoen_back.dto.payment.settlement;

import java.time.LocalDateTime;

public record SettlementUserDetailsDto(
        String senderNickname,  // 돈을 받을 사람
        Long paymentId,
        String paymentName,
        String settlementName,
        Long amount,
        Boolean isPaid,
        LocalDateTime payTime
) {
    public SettlementUserDetailsDto(String senderNickname, Long amount) {
        this(senderNickname, null, null, null, amount, null, null);
    }
}
