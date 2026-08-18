package com.sallim.ocr.dto;

import java.util.List;

/**
 * CLOVA General OCR API 요청 body.
 * 공식 스펙: version/requestId/timestamp/lang/images 구조.
 */
public record ClovaOcrRequest(
        String version,
        String requestId,
        long timestamp,
        String lang,
        List<Image> images
) {

    public record Image(
            String format,
            String name,
            String data // base64 인코딩된 이미지
    ) {
    }
}
