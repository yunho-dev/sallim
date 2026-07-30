package com.sallim.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "은행을 선택해주세요.")
        String bankCode,

        @NotBlank(message = "계좌번호는 필수입니다.")
        @Size(max = 34, message = "계좌번호는 34자를 초과할 수 없습니다.")
        String accountNo,

        @NotBlank(message = "계좌 별명은 필수입니다.")
        @Size(max = 40, message = "계좌 별명은 40자를 초과할 수 없습니다.")
        String accountName,

        @NotNull(message = "초기 잔액은 필수입니다.")
        @PositiveOrZero(message = "초기 잔액은 0 이상이어야 합니다.")
        BigDecimal balance
) {
}