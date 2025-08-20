package com.yoen.yoen_back.dto.payment.settlement;

import com.yoen.yoen_back.enums.PaymentType;

import java.util.List;

public record SettlementPaymentTypeDto(
        PaymentType paymentType, List<SettlementResponseUserDetailDto> settlementList
) {
}
