package com.sallim.dashboard.service;

import com.querydsl.core.Tuple;
import com.sallim.account.repository.AccountRepository;
import com.sallim.category.entity.CategoryType;
import com.sallim.dashboard.dto.CategoryExpenseResponse;
import com.sallim.dashboard.dto.DashboardSummaryResponse;
import com.sallim.dashboard.dto.MonthlyTrendResponse;
import com.sallim.dashboard.repository.DashboardQueryRepository;
import com.sallim.member.entity.Member;
import com.sallim.member.repository.MemberRepository;
import com.sallim.transaction.dto.TransactionResponse;
import com.sallim.transaction.entity.Transaction;
import com.sallim.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_TRANSACTION_LIMIT = 6;
    private static final int MONTHLY_TREND_MONTHS = 6;
    private static final int CATEGORY_TOP_N = 5;
    private static final String ETC_CATEGORY_NAME = "기타";

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DashboardQueryRepository dashboardQueryRepository;

    public DashboardSummaryResponse getSummary(String memberId, int year, int month) {
        Member member = getMember(memberId);

        YearMonth thisMonth = YearMonth.of(year, month);
        YearMonth lastMonth = thisMonth.minusMonths(1);

        BigDecimal thisMonthIncome = sumByPeriod(member, CategoryType.INCOME, thisMonth);
        BigDecimal thisMonthExpense = sumByPeriod(member, CategoryType.EXPENSE, thisMonth);
        BigDecimal lastMonthIncome = sumByPeriod(member, CategoryType.INCOME, lastMonth);
        BigDecimal lastMonthExpense = sumByPeriod(member, CategoryType.EXPENSE, lastMonth);
        BigDecimal totalBalance = accountRepository.sumBalanceByMemberAndIsDeletedFalse(member);

        return new DashboardSummaryResponse(thisMonthIncome, thisMonthExpense, lastMonthIncome, lastMonthExpense, totalBalance);
    }

    // 대시보드에 표시 중인 월부터 과거 6개월 - 거래가 없는 달도 0으로 채워서 항상 6개 항목을 반환 (프론트 차트가 고정 6칸을 기대)
    public List<MonthlyTrendResponse> getMonthlyTrend(String memberId, int year, int month) {
        Member member = getMember(memberId);

        YearMonth to = YearMonth.of(year, month);
        YearMonth from = to.minusMonths(MONTHLY_TREND_MONTHS - 1L);

        Map<String, BigDecimal[]> incomeExpenseByMonth = new LinkedHashMap<>();
        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {
            incomeExpenseByMonth.put(ym.toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        List<Tuple> rows = dashboardQueryRepository.findMonthlyTrend(member, from.atDay(1), to.atEndOfMonth());
        for (Tuple row : rows) {
            String yearMonth = row.get(0, String.class);
            CategoryType type = row.get(1, CategoryType.class);
            BigDecimal amount = row.get(2, BigDecimal.class);

            BigDecimal[] incomeExpense = incomeExpenseByMonth.get(yearMonth);
            if (incomeExpense == null) {
                continue;
            }
            if (type == CategoryType.INCOME) {
                incomeExpense[0] = amount;
            } else {
                incomeExpense[1] = amount;
            }
        }

        List<MonthlyTrendResponse> result = new ArrayList<>();
        incomeExpenseByMonth.forEach((yearMonth, incomeExpense) ->
                result.add(new MonthlyTrendResponse(yearMonth, incomeExpense[0], incomeExpense[1])));
        return result;
    }

    // 표시 중인 월의 지출 상위 5개 카테고리 + 나머지는 "기타"로 합산
    public List<CategoryExpenseResponse> getCategoryExpense(String memberId, int year, int month) {
        Member member = getMember(memberId);
        YearMonth targetMonth = YearMonth.of(year, month);

        List<Tuple> rows = dashboardQueryRepository.findCategoryExpense(member, targetMonth.atDay(1), targetMonth.atEndOfMonth());

        BigDecimal total = rows.stream()
                .map(row -> row.get(1, BigDecimal.class))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<CategoryExpenseResponse> result = new ArrayList<>();
        BigDecimal etcAmount = BigDecimal.ZERO;

        for (int i = 0; i < rows.size(); i++) {
            String categoryName = rows.get(i).get(0, String.class);
            BigDecimal amount = rows.get(i).get(1, BigDecimal.class);

            if (i < CATEGORY_TOP_N) {
                result.add(new CategoryExpenseResponse(categoryName, amount, toPercentage(amount, total)));
            } else {
                etcAmount = etcAmount.add(amount);
            }
        }

        if (etcAmount.compareTo(BigDecimal.ZERO) > 0) {
            result.add(new CategoryExpenseResponse(ETC_CATEGORY_NAME, etcAmount, toPercentage(etcAmount, total)));
        }

        return result;
    }

    // 표시 중인 월의 최근 거래 - 다른 달을 선택했을 때 오늘 날짜 기준 거래가 섞여 보이지 않도록 월로 스코프
    public List<TransactionResponse> getRecentTransactions(String memberId, int year, int month) {
        Member member = getMember(memberId);
        YearMonth targetMonth = YearMonth.of(year, month);

        List<Transaction> transactions = dashboardQueryRepository.findRecentTransactions(
                member, targetMonth.atDay(1), targetMonth.atEndOfMonth(), RECENT_TRANSACTION_LIMIT);
        return transactions.stream().map(TransactionResponse::from).toList();
    }

    private BigDecimal sumByPeriod(Member member, CategoryType type, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        return transactionRepository.sumAmountByMemberAndTypeAndPeriod(member, type, from, to);
    }

    private double toPercentage(BigDecimal amount, BigDecimal total) {
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Member getMember(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
}
