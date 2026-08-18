package com.sallim.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * CLOVA General OCR API 응답 body.
 * validationResult, convertedImageInfo 등 안 쓰는 필드가 섞여 있어서
 * ImageResult에 ignoreUnknown = true를 걸어둠 (안 그러면 Jackson이 역직렬화 시 예외를 던짐).
 */
public record ClovaOcrResponse(
        String version,
        String requestId,
        long timestamp,
        List<ImageResult> images
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageResult(
            String uid,
            String name,
            String inferResult, // "SUCCESS" 인지 반드시 체크
            String message,
            List<Field> fields
    ) {
    }

    public record Field(
            String valueType,
            BoundingPoly boundingPoly,
            String inferText,
            double inferConfidence, // 0.0 ~ 1.0, 낮으면 신뢰하면 안 됨
            String type,
            boolean lineBreak
    ) {
    }

    public record BoundingPoly(List<Vertex> vertices) {
    }

    public record Vertex(double x, double y) {
    }
}
