package com.sallim.account.controller;

import com.sallim.account.dto.AccountRequest;
import com.sallim.account.dto.AccountResponse;
import com.sallim.account.dto.AccountUpdateRequest;
import com.sallim.account.dto.BankResponse;
import com.sallim.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountApiController {

    private final AccountService accountService;

    // 조회
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(Authentication authentication) {
        List<AccountResponse> accounts = accountService.getAccounts(authentication.getName());
        return ResponseEntity.ok(accounts);
    }

    // 계좌 추가 모달의 은행 선택지 채우는 용도
    @GetMapping("/banks")
    public ResponseEntity<List<BankResponse>> getBanks() {
        return ResponseEntity.ok(accountService.getBanks());
    }

    // 추가
    @PostMapping
    public ResponseEntity<Void> createAccount(Authentication authentication, @Valid @RequestBody AccountRequest request) {
        accountService.createAccount(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 수정 (별명만 변경 가능)
    @PutMapping("/{accountId}")
    public ResponseEntity<Void> updateAccount(Authentication authentication, @PathVariable Long accountId,
                                               @Valid @RequestBody AccountUpdateRequest request) {
        accountService.updateAccount(authentication.getName(), accountId, request);
        return ResponseEntity.ok().build();
    }

    // 삭제 (soft delete)
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, @PathVariable Long accountId) {
        accountService.deleteAccount(authentication.getName(), accountId);
        return ResponseEntity.noContent().build();
    }

}