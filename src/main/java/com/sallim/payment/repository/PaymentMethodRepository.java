package com.sallim.payment.repository;

import com.sallim.member.entity.Member;
import com.sallim.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByMemberAndIsDeletedFalseOrderByInsertDateDesc(Member member);

    Optional<PaymentMethod> findByPaymentMethodIdAndMemberAndIsDeletedFalse(Long paymentMethodId, Member member);

}
