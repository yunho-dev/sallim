package com.sallim.payment.dto;

import com.sallim.payment.entity.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 생성/수정 공용 DTO - type/이름/연결계좌/메모 전부 언제든 바꿀 수 있는 메타데이터라
// Account처럼 수정 범위를 따로 제한할 이유가 없어 Category와 동일하게 하나로 합쳤다.
public record PaymentMethodRequest(
        @NotNull(message = "결제수단 유형은 필수입니다.")
        PaymentMethodType type,

        @NotBlank(message = "결제수단 이름은 필수입니다.")
        @Size(max = 40, message = "결제수단 이름은 40자를 초과할 수 없습니다.")
        String paymentMethodName,

        // 카드/현금처럼 특정 계좌와 무관한 결제수단은 null로 보내면 "연결 안 함" 처리
        Long accountId,

        @Size(max = 4000, message = "메모는 4000자를 초과할 수 없습니다.")
        String memo
) {
}
