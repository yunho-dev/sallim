package com.sallim.ocr.dto;

/**
 * OCR 원본 응답을 파싱해서 뽑아낸 최종 결과.
 * null인 필드가 있으면 프론트에서 "직접 입력해주세요" 폴백
 */
public record ReceiptParseResult(
        String merchantName,
        String transactionDate, // yyyy-MM-dd 형식으로 정규화됨
        Long amount
) {
}
