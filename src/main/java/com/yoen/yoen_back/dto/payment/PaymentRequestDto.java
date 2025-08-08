package com.yoen.yoen_back.dto.payment;

import com.yoen.yoen_back.dto.payment.settlement.SettlementRequestDto;
import com.yoen.yoen_back.enums.Currency;
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;
import com.yoen.yoen_back.enums.PaymentType;

import java.util.List;

public record PaymentRequestDto(Long paymentId, Long travelId, Long travelUserId, Long categoryId, Payer payerType, String payTime,
                                Long paymentAccount, String paymentName, PaymentMethod paymentMethod, Currency currency,
                                List<SettlementRequestDto> settlementList, PaymentType paymentType) {
}