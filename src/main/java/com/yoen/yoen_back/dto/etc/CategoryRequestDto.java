package com.yoen.yoen_back.dto.etc;

import com.yoen.yoen_back.enums.PaymentType;

public record CategoryRequestDto(Long categoryId, String categoryName, PaymentType categoryType) {
}
