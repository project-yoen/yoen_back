package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.enums.Payer;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponseDto (Long paymentId, Long categoryId, Payer payerType, LocalDateTime payTime, Long paymentAccount, List<PaymentImageDto> images){
}
