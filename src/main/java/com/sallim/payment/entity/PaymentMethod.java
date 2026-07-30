package com.sallim.payment.entity;

import com.sallim.account.entity.Account;
import com.sallim.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT_METHOD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private PaymentMethodType type;

    // 카드/현금처럼 특정 계좌와 무관한 결제수단은 null - CASCADE 없이 매핑해서 계좌가 soft delete돼도
    // 과거에 이 결제수단으로 찍힌 거래 이력과의 연결이 끊기지 않게 함(RESTRICT).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "payment_method_name", nullable = false, length = 40)
    private String paymentMethodName;

    @Column(name = "memo", length = 4000)
    private String memo;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "insert_date", nullable = false, updatable = false)
    private LocalDateTime insertDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Builder
    public PaymentMethod(Member member, PaymentMethodType type, Account account,
                          String paymentMethodName, String memo) {
        this.member = member;
        this.type = type;
        this.account = account;
        this.paymentMethodName = paymentMethodName;
        this.memo = memo;
    }

    // type/연결계좌/이름/메모 전부 사용자가 언제든 바꿀 수 있는 메타데이터라 전체 필드를 수정 대상으로 둠
    // (계좌번호처럼 실물 식별자가 아니므로 Account.changeAccountName 같은 부분 수정 제한이 필요 없음)
    public void update(PaymentMethodType type, Account account, String paymentMethodName, String memo) {
        this.type = type;
        this.account = account;
        this.paymentMethodName = paymentMethodName;
        this.memo = memo;
    }

    public void delete() {
        this.isDeleted = true;
    }

}
