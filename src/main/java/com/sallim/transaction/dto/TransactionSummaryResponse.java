package com.sallim.transaction.dto;

import java.math.BigDecimal;

// 거래내역 페이지 상단 요약 카드(이번 달 수입/지출/순수익) 전용.
// 필터(카테고리/결제수단/유형)와 무관하게 항상 기간 전체 기준으로 계산한다 - 목업 UI에서도
// 요약 카드가 필터 바와 별개로 고정돼 있어서, 여기 세 값은 목록 필터 결과와 다를 수 있다.
public record TransactionSummaryResponse(
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal netTotal
) {
}
