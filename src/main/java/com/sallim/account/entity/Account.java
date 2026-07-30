package com.sallim.account.entity;

import com.sallim.global.util.AccountNumberConverter;
import com.sallim.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // CascadeType.REMOVE를 걸지 않은 게 의도한 설계: 계좌가 남아있는 은행/회원은 실수로라도
    // 하드 삭제되면 안 되고(RESTRICT), 계좌 자체도 아래 delete()로 soft delete만 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_code", nullable = false)
    private Bank bank;

    // 이 컬럼만 컨버터를 거쳐 암호화된다. 엔티티/서비스 코드 입장에서는 그냥 평문 String.
    @Convert(converter = AccountNumberConverter.class)
    @Column(name = "account_no", nullable = false, length = 255)
    private String accountNo;

    // account_no와 같은 값을 담지만 암호화하지 않는 HMAC 해시. account_no는 GCM 랜덤 IV 때문에
    // 암호문이 매번 달라져 "이미 등록된 계좌인지" 조회가 불가능해서, 결정론적으로 조회 가능한 이 컬럼을 따로 둔다.
    @Column(name = "account_no_hash", nullable = false, length = 64)
    private String accountNoHash;

    @Column(name = "account_name", nullable = false, length = 40)
    private String accountName;

    @Column(name = "balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal balance;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "insert_date", nullable = false, updatable = false)
    private LocalDateTime insertDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Builder
    public Account(Member member, Bank bank, String accountNo, String accountNoHash,
                    String accountName, BigDecimal balance) {
        this.member = member;
        this.bank = bank;
        this.accountNo = accountNo;
        this.accountNoHash = accountNoHash;
        this.accountName = accountName;
        this.balance = balance;
    }

    // 계좌번호/은행은 실제 계좌의 불변 식별자라 수정 대상에서 제외 - 별명(닉네임)만 바꿀 수 있게 열어둠
    public void changeAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void delete() {
        this.isDeleted = true;
    }

}