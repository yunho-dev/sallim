package com.sallim.account.dto;

import com.sallim.account.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long accountId,
        String bankCode,
        String bankName,
        String accountNoMasked,
        String accountName,
        BigDecimal balance,
        LocalDateTime insertDate,
        LocalDateTime updateDate
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getBank().getBankCode(),
                account.getBank().getBankName(),
                mask(account.getAccountNo()),
                account.getAccountName(),
                account.getBalance(),
                account.getInsertDate(),
                account.getUpdateDate()
        );
    }

    // 복호화된 평문 계좌번호는 API 응답으로 절대 그대로 내보내지 않는다 - 저장은 암호화되어 있어도
    // 응답 JSON은 네트워크 탭/로그에 남을 수 있어 그 경로에서도 원문이 드러나지 않게 마스킹한다.
    // 실제 화면 목업(예: "110-***-456789")보다 뒤 자리 수를 6자리 대신 4자리만 노출해 조금 더 보수적으로 가렸다.
    private static String mask(String plainAccountNo) {
        String digits = plainAccountNo.replaceAll("[^0-9]", "");
        if (digits.length() <= 7) {
            return "*".repeat(digits.length());
        }
        String first = digits.substring(0, 3);
        String last = digits.substring(digits.length() - 4);
        return first + "-***-" + last;
    }
}