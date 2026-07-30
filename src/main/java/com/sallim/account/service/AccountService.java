package com.sallim.account.service;

import com.sallim.account.dto.AccountRequest;
import com.sallim.account.dto.AccountResponse;
import com.sallim.account.dto.AccountUpdateRequest;
import com.sallim.account.dto.BankResponse;
import com.sallim.account.entity.Account;
import com.sallim.account.entity.Bank;
import com.sallim.account.repository.AccountRepository;
import com.sallim.account.repository.BankRepository;
import com.sallim.global.util.AccountNumberHasher;
import com.sallim.member.entity.Member;
import com.sallim.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BankRepository bankRepository;
    private final MemberRepository memberRepository;
    private final AccountNumberHasher accountNumberHasher;

    // 조회
    public List<AccountResponse> getAccounts(String memberId) {
        Member member = getMember(memberId);
        return accountRepository.findByMemberAndIsDeletedFalseOrderByInsertDateDesc(member)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    // 계좌 추가 모달의 은행 선택 목록용
    public List<BankResponse> getBanks() {
        return bankRepository.findAll()
                .stream()
                .map(BankResponse::from)
                .toList();
    }

    // 추가
    @Transactional
    public void createAccount(String memberId, @Valid AccountRequest request) {
        Member member = getMember(memberId);
        Bank bank = getBank(request.bankCode());
        String accountNoHash = accountNumberHasher.hash(request.accountNo());

        // 같은 은행의 같은 실물 계좌를 이 회원 명의로 중복 등록하는 것을 방지
        if (accountRepository.existsByMemberAndBankAndAccountNoHashAndIsDeletedFalse(member, bank, accountNoHash)) {
            throw new IllegalArgumentException("이미 등록된 계좌입니다.");
        }

        Account account = Account.builder()
                .member(member)
                .bank(bank)
                .accountNo(request.accountNo())
                .accountNoHash(accountNoHash)
                .accountName(request.accountName())
                .balance(request.balance())
                .build();

        accountRepository.save(account);
    }

    // 수정 (별명만 변경 가능)
    @Transactional
    public void updateAccount(String memberId, Long accountId, @Valid AccountUpdateRequest request) {
        Account account = getOwnedAccount(memberId, accountId);
        account.changeAccountName(request.accountName());
    }

    // 삭제 (soft delete)
    @Transactional
    public void deleteAccount(String memberId, Long accountId) {
        Account account = getOwnedAccount(memberId, accountId);
        account.delete();
    }

    private Member getMember(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private Bank getBank(String bankCode) {
        return bankRepository.findById(bankCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 은행입니다."));
    }

    // 본인 소유 계좌인지 함께 확인 (타인의 accountId로 접근하는 것을 방지)
    private Account getOwnedAccount(String memberId, Long accountId) {
        Member member = getMember(memberId);
        return accountRepository.findByAccountIdAndMemberAndIsDeletedFalse(accountId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));
    }
}