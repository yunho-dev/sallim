package com.sallim.transaction.dto;

import com.sallim.category.entity.CategoryType;
import com.sallim.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        CategoryType type,
        Long categoryId,
        String categoryName,
        String categoryIconKey,
        Long paymentMethodId,
        String paymentMethodName,
        BigDecimal amount,
        LocalDate transactionDate,
        LocalDate settlementDate,
        String description,
        LocalDateTime insertDate
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getType(),
                transaction.getCategory().getCategoryId(),
                transaction.getCategory().getCategoryName(),
                transaction.getCategory().getIconKey(),
                transaction.getPaymentMethod().getPaymentMethodId(),
                transaction.getPaymentMethod().getPaymentMethodName(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getSettlementDate(),
                transaction.getDescription(),
                transaction.getInsertDate()
        );
    }
}
