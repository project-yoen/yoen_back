package com.yoen.yoen_back.dto.payment;

import com.yoen.yoen_back.enums.Payer;

import java.time.LocalDateTime;

public record PaymentSimpleResponseDto(Long paymentId, String paymentName, String categoryName, LocalDateTime payTime, String payer, Long paymentAccount, Payer payerType) {
}
