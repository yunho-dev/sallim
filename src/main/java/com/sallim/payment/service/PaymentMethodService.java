package com.sallim.payment.service;

import com.sallim.account.entity.Account;
import com.sallim.account.repository.AccountRepository;
import com.sallim.member.entity.Member;
import com.sallim.member.repository.MemberRepository;
import com.sallim.payment.dto.PaymentMethodRequest;
import com.sallim.payment.dto.PaymentMethodResponse;
import com.sallim.payment.entity.PaymentMethod;
import com.sallim.payment.repository.PaymentMethodRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    // 목록 조회
    public List<PaymentMethodResponse> getPaymentMethods(String memberId) {
        Member member = getMember(memberId);
        return paymentMethodRepository.findByMemberAndIsDeletedFalseOrderByInsertDateDesc(member)
                .stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    // 단건 조회
    public PaymentMethodResponse getPaymentMethod(String memberId, Long paymentMethodId) {
        PaymentMethod paymentMethod = getOwnedPaymentMethod(memberId, paymentMethodId);
        return PaymentMethodResponse.from(paymentMethod);
    }

    // 추가
    @Transactional
    public void createPaymentMethod(String memberId, @Valid PaymentMethodRequest request) {
        Member member = getMember(memberId);
        Account account = getOwnedAccountOrNull(member, request.accountId());

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .member(member)
                .type(request.type())
                .account(account)
                .paymentMethodName(request.paymentMethodName())
                .memo(request.memo())
                .build();

        paymentMethodRepository.save(paymentMethod);
    }

    // 수정
    @Transactional
    public void updatePaymentMethod(String memberId, Long paymentMethodId, @Valid PaymentMethodRequest request) {
        PaymentMethod paymentMethod = getOwnedPaymentMethod(memberId, paymentMethodId);
        Account account = getOwnedAccountOrNull(paymentMethod.getMember(), request.accountId());

        paymentMethod.update(request.type(), account, request.paymentMethodName(), request.memo());
    }

    // 삭제 (soft delete)
    @Transactional
    public void deletePaymentMethod(String memberId, Long paymentMethodId) {
        PaymentMethod paymentMethod = getOwnedPaymentMethod(memberId, paymentMethodId);
        paymentMethod.delete();
    }

    private Member getMember(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // 본인 소유 결제수단인지 함께 확인 (타인의 paymentMethodId로 접근하는 것을 방지)
    private PaymentMethod getOwnedPaymentMethod(String memberId, Long paymentMethodId) {
        Member member = getMember(memberId);
        return paymentMethodRepository.findByPaymentMethodIdAndMemberAndIsDeletedFalse(paymentMethodId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제수단입니다."));
    }

    // accountId가 없으면(카드/현금 등) null 그대로 반환, 있으면 본인 소유 계좌인지 검증
    private Account getOwnedAccountOrNull(Member member, Long accountId) {
        if (accountId == null) {
            return null;
        }
        return accountRepository.findByAccountIdAndMemberAndIsDeletedFalse(accountId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));
    }
}
