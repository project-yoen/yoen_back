package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Payer;

import java.time.LocalDateTime;
import java.util.List;
//Todo: categoryId 말고 Category name이나 type을 반환하게 변경
public record PaymentResponseDto (Long paymentId, Long categoryId, Payer payerType, LocalDateTime payTime, Long paymentAccount, List<PaymentImageDto> images){
}
