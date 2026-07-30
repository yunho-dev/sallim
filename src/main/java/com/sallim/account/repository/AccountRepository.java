package com.sallim.account.repository;

import com.sallim.account.entity.Account;
import com.sallim.account.entity.Bank;
import com.sallim.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByMemberAndIsDeletedFalseOrderByInsertDateDesc(Member member);

    Optional<Account> findByAccountIdAndMemberAndIsDeletedFalse(Long accountId, Member member);

    // 같은 실물 계좌를 중복 등록하는 것을 막기 위한 조회 - account_no는 암호화되어 있어 직접 비교가
    // 불가능하므로 결정론적으로 같은 값이 나오는 account_no_hash로 대신 비교한다.
    boolean existsByMemberAndBankAndAccountNoHashAndIsDeletedFalse(Member member, Bank bank, String accountNoHash);

}