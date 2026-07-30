package com.sallim.payment.controller;

import com.sallim.payment.dto.PaymentMethodRequest;
import com.sallim.payment.dto.PaymentMethodResponse;
import com.sallim.payment.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodApiController {

    private final PaymentMethodService paymentMethodService;

    // 목록 조회
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(Authentication authentication) {
        List<PaymentMethodResponse> paymentMethods = paymentMethodService.getPaymentMethods(authentication.getName());
        return ResponseEntity.ok(paymentMethods);
    }

    // 단건 조회
    @GetMapping("/{paymentMethodId}")
    public ResponseEntity<PaymentMethodResponse> getPaymentMethod(Authentication authentication, @PathVariable Long paymentMethodId) {
        PaymentMethodResponse paymentMethod = paymentMethodService.getPaymentMethod(authentication.getName(), paymentMethodId);
        return ResponseEntity.ok(paymentMethod);
    }

    // 추가
    @PostMapping
    public ResponseEntity<Void> createPaymentMethod(Authentication authentication, @Valid @RequestBody PaymentMethodRequest request) {
        paymentMethodService.createPaymentMethod(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 수정
    @PutMapping("/{paymentMethodId}")
    public ResponseEntity<Void> updatePaymentMethod(Authentication authentication, @PathVariable Long paymentMethodId,
                                                     @Valid @RequestBody PaymentMethodRequest request) {
        paymentMethodService.updatePaymentMethod(authentication.getName(), paymentMethodId, request);
        return ResponseEntity.ok().build();
    }

    // 삭제 (soft delete)
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(Authentication authentication, @PathVariable Long paymentMethodId) {
        paymentMethodService.deletePaymentMethod(authentication.getName(), paymentMethodId);
        return ResponseEntity.noContent().build();
    }

}
