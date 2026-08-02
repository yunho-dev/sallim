package com.sallim.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyTrendResponse(
        String yearMonth,
        BigDecimal income,
        BigDecimal expense
) {
}
