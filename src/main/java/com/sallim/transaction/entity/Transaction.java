package com.sallim.transaction.entity;

import com.sallim.category.entity.Category;
import com.sallim.category.entity.CategoryType;
import com.sallim.payment.entity.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    // transaction 테이블엔 member_id가 직접 없다 - 소유자는 paymentMethod.member를 통해서만 알 수 있음
    // (실제 DDL 확인 결과. CASCADE 없이 매핑해서 결제수단이 soft delete돼도 거래 기록은 그대로 RESTRICT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Category.categoryType과 값 집합이 완전히 같아서(EXPENSE/INCOME) 새 enum을 안 만들고 그대로 재사용.
    // category_id로도 유형을 알 수 있지만 컬럼이 따로 있는 이유는 조회/필터 시 category 조인 없이
    // 바로 비교할 수 있게 하기 위한 의도적 비정규화로 보임 - 그래서 정합성은 서비스 레이어에서 검증한다
    // (요청의 type이 선택한 category의 categoryType과 다르면 생성/수정 자체를 막음).
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private CategoryType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    // 카드 결제처럼 실제 출금이 거래일보다 늦는 경우를 위한 컬럼. 현재 UI에는 입력 폼이 없어 항상 null로 들어감.
    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // 실제 DB 컬럼은 timestamp without time zone (ERD엔 date로 잘못 표기돼 있었음 - psql로 직접 확인)
    @CreationTimestamp
    @Column(name = "insert_date", nullable = false, updatable = false)
    private LocalDateTime insertDate;

    @Builder
    public Transaction(PaymentMethod paymentMethod, Category category, CategoryType type, BigDecimal amount,
                        LocalDate transactionDate, LocalDate settlementDate, String description) {
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.settlementDate = settlementDate;
        this.description = description;
    }

    public void update(PaymentMethod paymentMethod, Category category, CategoryType type, BigDecimal amount,
                        LocalDate transactionDate, LocalDate settlementDate, String description) {
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.settlementDate = settlementDate;
        this.description = description;
    }

    public void delete() {
        this.isDeleted = true;
    }

}
