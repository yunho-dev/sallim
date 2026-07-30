package com.sallim.payment.dto;

import com.sallim.payment.entity.PaymentMethod;
import com.sallim.payment.entity.PaymentMethodType;

import java.time.LocalDateTime;

public record PaymentMethodResponse(
        Long paymentMethodId,
        PaymentMethodType type,
        String paymentMethodName,
        Long accountId,
        String accountName,
        String memo,
        LocalDateTime insertDate,
        LocalDateTime updateDate
) {
    public static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        // account 미연결(카드/현금)이면 accountId/accountName 모두 null로 내려감
        Long accountId = paymentMethod.getAccount() != null ? paymentMethod.getAccount().getAccountId() : null;
        String accountName = paymentMethod.getAccount() != null ? paymentMethod.getAccount().getAccountName() : null;

        return new PaymentMethodResponse(
                paymentMethod.getPaymentMethodId(),
                paymentMethod.getType(),
                paymentMethod.getPaymentMethodName(),
                accountId,
                accountName,
                paymentMethod.getMemo(),
                paymentMethod.getInsertDate(),
                paymentMethod.getUpdateDate()
        );
    }
}
