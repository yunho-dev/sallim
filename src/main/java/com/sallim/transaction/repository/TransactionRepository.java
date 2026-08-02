package com.sallim.transaction.repository;

import com.sallim.category.entity.CategoryType;
import com.sallim.member.entity.Member;
import com.sallim.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 본인 소유 거래인지 함께 확인 (transaction엔 member_id가 없어 paymentMethod.member를 경유)
    Optional<Transaction> findByTransactionIdAndPaymentMethod_MemberAndIsDeletedFalse(Long transactionId, Member member);

    // 거래내역 페이지 필터 바(기간/카테고리/결제수단/유형)를 그대로 반영한 다중 조건 페이징 조회.
    // categoryId/paymentMethodId/type은 선택값이라 "파라미터가 null이면 그 조건은 무시" 패턴(:x IS NULL OR ...)을 사용 -
    // 이 프로젝트엔 아직 Specification/QueryDSL 같은 동적 쿼리 도구가 없어서, 새 의존성 없이 순수 JPQL로 처리했다.
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.paymentMethod.member = :member
              AND t.isDeleted = false
              AND t.transactionDate BETWEEN :from AND :to
              AND (:categoryId IS NULL OR t.category.categoryId = :categoryId)
              AND (:paymentMethodId IS NULL OR t.paymentMethod.paymentMethodId = :paymentMethodId)
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.transactionDate DESC, t.transactionId DESC
            """)
    Page<Transaction> search(@Param("member") Member member,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("categoryId") Long categoryId,
                              @Param("paymentMethodId") Long paymentMethodId,
                              @Param("type") CategoryType type,
                              Pageable pageable);

    // 거래내역 페이지의 "이번 달 수입/지출" 요약 카드용. 페이징된 목록만 합산하면 현재 페이지분만 더해져
    // 틀린 값이 나오므로, 기간 전체를 별도로 합산하는 쿼리를 둔다. 같은 (member, type, 기간) 조합을
    // 대시보드의 월별/기간별 KPI 계산에도 그대로 재사용할 수 있다.
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.paymentMethod.member = :member
              AND t.isDeleted = false
              AND t.type = :type
              AND t.transactionDate BETWEEN :from AND :to
            """)
    BigDecimal sumAmountByMemberAndTypeAndPeriod(@Param("member") Member member,
                                                  @Param("type") CategoryType type,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

}
