package com.sallim.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal thisMonthIncome,
        BigDecimal thisMonthExpense,
        BigDecimal lastMonthIncome,
        BigDecimal lastMonthExpense,
        BigDecimal totalBalance
) {
}
