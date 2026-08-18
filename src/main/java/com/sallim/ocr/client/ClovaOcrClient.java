package com.sallim.ocr.client;

import com.sallim.config.ClovaConfig.ClovaOcrProperties;
import com.sallim.ocr.dto.ClovaOcrRequest;
import com.sallim.ocr.dto.ClovaOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * CLOVA General OCR API를 호출하는 순수 HTTP 클라이언트.
 * 여기서는 "호출"만 책임지고, 파싱 로직은 parser 패키지로 분리했음 (관심사 분리).
 *
 * RestTemplate 대신 RestClient를 쓴 이유:
 * - RestTemplate은 Spring 5.0부터 유지보수 모드(maintenance mode)라 신규 기능 추가가 없음
 * - WebClient는 리액티브(WebFlux) 환경에 최적화된 API라, MVC 기반인 우리 프로젝트에선
 *   동기 호출인데도 불필요하게 복잡함 (Mono/Flux 다뤄야 함)
 * - RestClient는 Spring 6.1(Boot 3.2+)에서 나온, 동기 호출을 위한 최신 fluent API
 */
@Component
@RequiredArgsConstructor
public class ClovaOcrClient {

    private final ClovaOcrProperties ocrProperties;
    private final RestClient restClient = RestClient.create();

    public ClovaOcrResponse recognizeText(byte[] imageBytes, String format) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes); // Base64 인코딩(서버로 보낼 데이터 준비 1)

        ClovaOcrRequest request = new ClovaOcrRequest(
                "V2",
                UUID.randomUUID().toString(),
                Instant.now().toEpochMilli(),
                "ko",
                List.of(new ClovaOcrRequest.Image(format, "receipt", base64Image))
        ); // 요청 객체 조립(서버로 보낼 데이터 준비 2)

        return restClient.post()
                .uri(ocrProperties.invokeUrl())
                .header("X-OCR-SECRET", ocrProperties.secretKey()) // 시크릿 키 추가
                .contentType(MediaType.APPLICATION_JSON) // JSON으로 보낸다고 표시
                .body(request)
                .retrieve() // 네이버 서버로 전송
                .body(ClovaOcrResponse.class); // 응답을 ClovaOcrResponse로 변환
    }
}
