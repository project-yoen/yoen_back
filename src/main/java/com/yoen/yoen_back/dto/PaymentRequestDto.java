package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;

public record PaymentRequestDto(Long paymentId, Long travelId, Long categoryId, Payer payerType, String payTime,
                                Long paymentAccount, String paymentName, PaymentMethod paymentMethod) {
}

