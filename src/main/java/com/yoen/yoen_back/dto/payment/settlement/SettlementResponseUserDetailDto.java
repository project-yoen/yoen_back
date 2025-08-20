package com.yoen.yoen_back.dto.payment.settlement;

import java.util.List;

public record SettlementResponseUserDetailDto(
        String receiverNickname, List<SettlementUserDetailsDto> userSettlementList
) {
}
