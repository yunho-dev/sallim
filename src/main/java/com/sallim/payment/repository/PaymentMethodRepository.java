package com.sallim.payment.repository;

import com.sallim.member.entity.Member;
import com.sallim.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByMemberAndIsDeletedFalseOrderByInsertDateDesc(Member member);

    Optional<PaymentMethod> findByPaymentMethodIdAndMemberAndIsDeletedFalse(Long paymentMethodId, Member member);

    // 목데이터 시더가 이미 있는 결제수단은 재사용하고 없을 때만 새로 만들기 위한 조회
    Optional<PaymentMethod> findByMemberAndPaymentMethodName(Member member, String paymentMethodName);

}
