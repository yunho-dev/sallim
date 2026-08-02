package com.sallim.dashboard.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sallim.category.entity.CategoryType;
import com.sallim.member.entity.Member;
import com.sallim.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.sallim.category.entity.QCategory.category;
import static com.sallim.transaction.entity.QTransaction.transaction;

// Dashboard는 엔티티가 아니라 여러 도메인(Transaction/Category)을 집계해 보여주는 화면 전용 조회 계층이라
// JpaRepository 상속 없이 JPAQueryFactory를 직접 쓰는 일반 리포지토리로 둔다.
@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 월별 수입/지출 추이 - to_char로 년-월 그룹핑 (PostgreSQL 전용 함수, 이 프로젝트는 DB를 PostgreSQL로 고정하므로 방언 분기 불필요)
    public List<Tuple> findMonthlyTrend(Member member, LocalDate from, LocalDate to) {
        return queryFactory
                .select(
                        Expressions.stringTemplate("to_char({0}, 'YYYY-MM')", transaction.transactionDate),
                        transaction.type,
                        transaction.amount.sum()
                )
                .from(transaction)
                .where(
                        transaction.paymentMethod.member.eq(member),
                        transaction.isDeleted.isFalse(),
                        transaction.transactionDate.between(from, to)
                )
                .groupBy(Expressions.stringTemplate("to_char({0}, 'YYYY-MM')", transaction.transactionDate), transaction.type)
                .fetch();
    }

    // 카테고리별 지출 비중 - 금액 내림차순으로 정렬해서 상위 N개 + 기타 합산은 서비스 계층에서 처리
    public List<Tuple> findCategoryExpense(Member member, LocalDate from, LocalDate to) {
        return queryFactory
                .select(category.categoryName, transaction.amount.sum())
                .from(transaction)
                .join(transaction.category, category)
                .where(
                        transaction.paymentMethod.member.eq(member),
                        transaction.isDeleted.isFalse(),
                        transaction.type.eq(CategoryType.EXPENSE),
                        transaction.transactionDate.between(from, to)
                )
                .groupBy(category.categoryName)
                .orderBy(transaction.amount.sum().desc())
                .fetch();
    }

    // 최근 거래 목록 - 대시보드에 표시 중인 월 기준으로 스코프
    public List<Transaction> findRecentTransactions(Member member, LocalDate from, LocalDate to, int limit) {
        return queryFactory
                .selectFrom(transaction)
                .where(
                        transaction.paymentMethod.member.eq(member),
                        transaction.isDeleted.isFalse(),
                        transaction.transactionDate.between(from, to)
                )
                .orderBy(transaction.transactionDate.desc(), transaction.transactionId.desc())
                .limit(limit)
                .fetch();
    }
}
