package com.sallim.ocr.service;

import com.sallim.ocr.client.ClovaOcrClient;
import com.sallim.ocr.dto.ClovaOcrResponse;
import com.sallim.ocr.dto.ClovaOcrResponse.Field;
import com.sallim.ocr.dto.ReceiptParseResult;
import com.sallim.ocr.parser.ReceiptFieldParser;
import com.sallim.ocr.parser.ReceiptTextLineBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 영수증 이미지 -> OCR 호출 -> 파싱까지의 전체 흐름을 조율하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

    private final ClovaOcrClient clovaOcrClient;
    private final ReceiptTextLineBuilder lineBuilder;
    private final ReceiptFieldParser fieldParser;

    public ReceiptParseResult parseReceipt(MultipartFile receiptImage) {
        byte[] imageBytes = readBytes(receiptImage); // 파일 > byte 배열
        String format = extractFormat(receiptImage.getOriginalFilename()); // 확장자 추출

        ClovaOcrResponse response = clovaOcrClient.recognizeText(imageBytes, format); // 네이버 서버 호출
        ClovaOcrResponse.ImageResult imageResult = firstImageResult(response);

        if (!"SUCCESS".equals(imageResult.inferResult())) {
            throw new IllegalStateException("OCR 인식에 실패했습니다: " + imageResult.message());
        }

        List<Field> fields = imageResult.fields();
        List<String> lines = lineBuilder.buildLines(fields);

        return fieldParser.parse(lines);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("영수증 이미지를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private ClovaOcrResponse.ImageResult firstImageResult(ClovaOcrResponse response) {
        if (response == null || response.images() == null || response.images().isEmpty()) {
            throw new IllegalStateException("OCR 응답이 비어있습니다.");
        }
        return response.images().get(0);
    }

    private String extractFormat(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // 확장자 없으면 기본값
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
