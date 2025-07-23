package com.yoen.yoen_back.dto.payment;

import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponseDto(Long paymentId, Long categoryId, String categoryName, Payer payerType,
                                 PaymentMethod paymentMethod, String paymentName, Double exchangeRate,
                                 LocalDateTime payTime, Long paymentAccount, List<PaymentImageDto> images) {
}
