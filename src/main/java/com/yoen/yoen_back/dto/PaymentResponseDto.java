package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.enums.Payer;

import java.util.List;

public record PaymentResponseDto (Long paymentId, Category category, Payer payerType, String payTime, Long paymentAccount, List<PaymentImageDto> images){
}
