package com.sallim.dashboard.controller;

import com.sallim.dashboard.dto.CategoryExpenseResponse;
import com.sallim.dashboard.dto.DashboardSummaryResponse;
import com.sallim.dashboard.dto.MonthlyTrendResponse;
import com.sallim.dashboard.service.DashboardService;
import com.sallim.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    // KPI 카드(선택한 달/전월 수입·지출, 현재 잔액)
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            Authentication authentication, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getSummary(authentication.getName(), year, month));
    }

    // 선택한 달부터 과거 6개월 수입/지출 추이 (바 차트)
    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrend(
            Authentication authentication, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrend(authentication.getName(), year, month));
    }

    // 선택한 달의 카테고리별 지출 비중 (도넛 차트)
    @GetMapping("/category-expense")
    public ResponseEntity<List<CategoryExpenseResponse>> getCategoryExpense(
            Authentication authentication, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getCategoryExpense(authentication.getName(), year, month));
    }

    // 선택한 달의 최근 거래 6건
    @GetMapping("/recent-transactions")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            Authentication authentication, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getRecentTransactions(authentication.getName(), year, month));
    }
}
