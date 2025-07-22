package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;

import java.util.List;

public record PaymentRequestDto(Long paymentId, Long travelId, Long categoryId, Payer payerType, String payTime,
                                Long paymentAccount, String paymentName, PaymentMethod paymentMethod,
                                List<SettlementRequestDto> settlementList) {
}

