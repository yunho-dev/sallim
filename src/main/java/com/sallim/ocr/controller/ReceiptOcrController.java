package com.sallim.ocr.controller;

import com.sallim.ocr.dto.ReceiptParseResult;
import com.sallim.ocr.service.ReceiptOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영수증 처리 컨트롤러
 */
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class ReceiptOcrController {

    private final ReceiptOcrService receiptOcrService;

    @PostMapping(value = "/receipt", consumes = "multipart/form-data")
    public ReceiptParseResult parseReceipt(@RequestParam("file") MultipartFile file) {
        return receiptOcrService.parseReceipt(file);
    }
}
