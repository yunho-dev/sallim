package com.sallim.transaction.repository;

import com.sallim.category.entity.CategoryType;
import com.sallim.member.entity.Member;
import com.sallim.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom {

    // 본인 소유 거래인지 함께 확인 (transaction엔 member_id가 없어 paymentMethod.member를 경유)
    Optional<Transaction> findByTransactionIdAndPaymentMethod_MemberAndIsDeletedFalse(Long transactionId, Member member);

    // 복구 대상 조회용 - 이미 삭제된 거래만 복구할 수 있어야 하므로 IsDeletedTrue로 한정
    Optional<Transaction> findByTransactionIdAndPaymentMethod_MemberAndIsDeletedTrue(Long transactionId, Member member);

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
