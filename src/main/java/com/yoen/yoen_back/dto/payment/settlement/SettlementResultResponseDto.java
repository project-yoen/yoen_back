package com.yoen.yoen_back.dto.payment.settlement;

import java.util.List;

public record SettlementResultResponseDto(
    List<SettlementResponseUserDetailDto> userSettlementList,  List<SettlementPaymentTypeDto> paymentTypeList
) {
}
