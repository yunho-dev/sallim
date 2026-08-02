package com.sallim.transaction.controller;

import com.sallim.category.entity.CategoryType;
import com.sallim.transaction.dto.TransactionRequest;
import com.sallim.transaction.dto.TransactionResponse;
import com.sallim.transaction.dto.TransactionSummaryResponse;
import com.sallim.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionApiController {

    private final TransactionService transactionService;

    // 목록 조회 (월 + 카테고리/결제수단/유형 필터, 페이징)
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) CategoryType type,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<TransactionResponse> transactions = transactionService.getTransactions(
                authentication.getName(), year, month, categoryId, paymentMethodId, type, pageable);
        return ResponseEntity.ok(transactions);
    }

    // 단건 조회
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(Authentication authentication, @PathVariable Long transactionId) {
        TransactionResponse transaction = transactionService.getTransaction(authentication.getName(), transactionId);
        return ResponseEntity.ok(transaction);
    }

    // 요약 카드(이번 달 수입/지출/순수익)
    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponse> getSummary(
            Authentication authentication, @RequestParam int year, @RequestParam int month) {
        TransactionSummaryResponse summary = transactionService.getSummary(authentication.getName(), year, month);
        return ResponseEntity.ok(summary);
    }

    // 추가
    @PostMapping
    public ResponseEntity<Void> createTransaction(Authentication authentication, @Valid @RequestBody TransactionRequest request) {
        transactionService.createTransaction(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 수정
    @PutMapping("/{transactionId}")
    public ResponseEntity<Void> updateTransaction(Authentication authentication, @PathVariable Long transactionId,
                                                   @Valid @RequestBody TransactionRequest request) {
        transactionService.updateTransaction(authentication.getName(), transactionId, request);
        return ResponseEntity.ok().build();
    }

    // 삭제 (soft delete)
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(Authentication authentication, @PathVariable Long transactionId) {
        transactionService.deleteTransaction(authentication.getName(), transactionId);
        return ResponseEntity.noContent().build();
    }

}
