package com.sallim.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 계좌 수정은 별명(닉네임)만 대상으로 한다 - 은행/계좌번호는 실물 계좌의 불변 식별자라 수정 범위에서 제외.
public record AccountUpdateRequest(
        @NotBlank(message = "계좌 별명은 필수입니다.")
        @Size(max = 40, message = "계좌 별명은 40자를 초과할 수 없습니다.")
        String accountName
) {
}