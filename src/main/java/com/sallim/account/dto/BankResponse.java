package com.sallim.account.dto;

import com.sallim.account.entity.Bank;

public record BankResponse(
        String bankCode,
        String bankName
) {
    public static BankResponse from(Bank bank) {
        return new BankResponse(bank.getBankCode(), bank.getBankName());
    }
}