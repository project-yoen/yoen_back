package com.yoen.yoen_back.dto.payment;

import com.yoen.yoen_back.dto.payment.settlement.SettlementResponseDto;
import com.yoen.yoen_back.dto.travel.TravelUserResponseDto;
import com.yoen.yoen_back.enums.Currency;
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;
import com.yoen.yoen_back.enums.PaymentType;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponseDto(Long travelId, Long paymentId, Long categoryId, String categoryName, Payer payerType, TravelUserResponseDto payerName,
                                 PaymentMethod paymentMethod, String paymentName, PaymentType paymentType, Double exchangeRate,
                                 LocalDateTime payTime, Long paymentAccount, Currency currency, List<SettlementResponseDto> settlements, List<PaymentImageDto> images) {
}
