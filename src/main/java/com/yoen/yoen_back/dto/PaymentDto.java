package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.enums.Payer;

public record PaymentDto(Long paymentId, Long travelId, Category category, Payer payerType, String payTime,
                         Long paymentAccount) {
}

