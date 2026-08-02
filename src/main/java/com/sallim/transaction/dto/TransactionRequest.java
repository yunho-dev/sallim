package com.sallim.transaction.dto;

import com.sallim.category.entity.CategoryType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// 생성/수정 공용 DTO (Category/PaymentMethod와 동일 패턴)
public record TransactionRequest(
        @NotNull(message = "거래 유형은 필수입니다.")
        CategoryType type,

        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId,

        @NotNull(message = "결제수단은 필수입니다.")
        Long paymentMethodId,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount,

        @NotNull(message = "거래일은 필수입니다.")
        LocalDate transactionDate,

        // 카드 실제 출금일 등에 쓰는 선택값 - 현재 UI엔 입력 폼이 없어 항상 null로 전달됨
        LocalDate settlementDate,

        @Size(max = 4000, message = "거래 내용은 4000자를 초과할 수 없습니다.")
        String description
) {
}
