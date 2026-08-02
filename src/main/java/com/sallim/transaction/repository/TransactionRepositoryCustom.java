package com.sallim.transaction.repository;

import com.sallim.category.entity.CategoryType;
import com.sallim.member.entity.Member;
import com.sallim.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransactionRepositoryCustom {

    // 거래내역 페이지 필터 바(기간/카테고리/결제수단/유형)를 그대로 반영한 다중 조건 페이징 조회.
    // categoryId/paymentMethodId/type은 선택값이라 QueryDSL BooleanBuilder로 동적 조건을 구성한다.
    Page<Transaction> search(Member member, LocalDate from, LocalDate to,
                              Long categoryId, Long paymentMethodId, CategoryType type,
                              Pageable pageable);
}
