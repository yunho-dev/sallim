package com.sallim.transaction.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sallim.category.entity.CategoryType;
import com.sallim.member.entity.Member;
import com.sallim.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.sallim.transaction.entity.QTransaction.transaction;

// Spring Data 커스텀 리포지토리 구현 - 클래스명은 반드시 "TransactionRepository" + "Impl"이어야
// Spring Data가 TransactionRepositoryCustom의 구현체로 자동 인식한다.
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Transaction> search(Member member, LocalDate from, LocalDate to,
                                     Long categoryId, Long paymentMethodId, CategoryType type,
                                     Pageable pageable) {
        BooleanBuilder condition = new BooleanBuilder()
                .and(transaction.paymentMethod.member.eq(member))
                .and(transaction.isDeleted.isFalse())
                .and(transaction.transactionDate.between(from, to));

        if (categoryId != null) {
            condition.and(transaction.category.categoryId.eq(categoryId));
        }
        if (paymentMethodId != null) {
            condition.and(transaction.paymentMethod.paymentMethodId.eq(paymentMethodId));
        }
        if (type != null) {
            condition.and(transaction.type.eq(type));
        }

        List<Transaction> content = queryFactory
                .selectFrom(transaction)
                .where(condition)
                .orderBy(transaction.transactionDate.desc(), transaction.transactionId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(transaction.transactionId.count())
                .from(transaction)
                .where(condition);

        // 마지막 페이지거나 데이터가 페이지 크기보다 적으면 count 쿼리를 생략해서 불필요한 조회를 줄인다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
