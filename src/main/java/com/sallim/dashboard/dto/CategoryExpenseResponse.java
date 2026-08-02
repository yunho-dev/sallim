package com.sallim.dashboard.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String categoryName,
        BigDecimal amount,
        double percentage
) {
}
