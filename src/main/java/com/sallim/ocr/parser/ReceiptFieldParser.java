package com.sallim.ocr.parser;

import com.sallim.ocr.dto.ReceiptParseResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영수증에서 추출한 재조립된 텍스트 줄들에서 정규식 + 휴리스틱으로 가맹점명/날짜/금액을 뽑아냄.
 * CLOVA의 특화 모델(ML 기반 key-value 추출)은 이용량과 무관하게 월 18,000원 고정비가 발생해서,
 * 개인용 저사용량 서비스 특성상 비효율적이라 판단해 규칙을 직접 코드로 작성함.
 * 새로운 영수증 포맷에서 실패하는 케이스가 나올 때마다 이 클래스의 규칙을 계속 보강해야 함.
 */
@Component
public class ReceiptFieldParser {

    // 사업자등록번호 형식: XXX-XX-XXXXX (상호명은 보통 이 줄 바로 위에 있음)
    private static final Pattern BUSINESS_NO_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{5}");

    // 날짜: 2자리/4자리 연도 둘 다 지원 (실제 영수증에서 "26-08-18" 형태 확인됨)
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{2}|\\d{4})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})");

    // 금액 관련 키워드가 있는 줄을 우선 탐색
    private static final Pattern AMOUNT_KEYWORD_PATTERN = Pattern.compile("(합계|총액|결제금액|금액|구매액|받을금액|청구금액|실결제)");

    // 콤마 포함 숫자 4자리 이상 (너무 짧은 숫자는 금액이 아닐 확률이 높아서 제외)
    private static final Pattern AMOUNT_NUMBER_PATTERN = Pattern.compile("[\\d,]{4,}");

    // 상품 테이블의 헤더 줄(단가/수량 등)은 "금액"이라는 열 제목만 있고 실제 총액이 아닌
    // 경우가 많아서, 금액 후보에서 미리 제외
    private static final Pattern TABLE_HEADER_PATTERN = Pattern.compile("단가.*수량|수량.*단가|품명.*수량");

    // 상호명 후보 줄에 주소/전화/대표자 정보가 섞여 있으면 노이즈로 간주 (아래 extractMerchantName 참고)
    private static final Pattern MERCHANT_NOISE_PATTERN = Pattern.compile("(주소|대표|전화|TEL|Tel)");

    // 사업자번호 바로 위 줄이 노이즈일 때, 몇 줄까지 더 위로 올라가며 상호명을 찾아볼지
    private static final int MERCHANT_LOOKBACK_LIMIT = 3;

    // 한 줄에 숫자 그룹이 이 개수보다 많으면 "여러 상품이 뭉친 표"로 간주하고 금액 후보에서 제외
    // (ReceiptTextLineBuilder의 겹침 판정이 촘촘한 상품 목록 표에서 여러 줄을 한 줄로 잘못 합치는
    //  경우가 실제로 있었음 - 그런 줄은 숫자가 비정상적으로 많이 섞여 있다는 특징으로 걸러낸다)
    private static final int MAX_NUMBERS_PER_LINE = 5;

    public ReceiptParseResult parse(List<String> lines) {
        String merchantName = extractMerchantName(lines);
        String date = extractDate(lines);
        Long amount = extractAmount(lines);
        return new ReceiptParseResult(merchantName, date, amount);
    }

    /**
     * 사업자등록번호가 있는 줄 바로 위 줄을 상호명으로 추정.
     * (실제 테스트에서 "첫 줄 = 상호명" 가정이 틀렸음을 확인함 - 광고성 안내문이
     *  최상단에 오는 영수증도 있어서, 사업자번호 앵커링이 훨씬 안정적)
     *
     * 다만 줄 재조립(ReceiptTextLineBuilder) 과정에서 인접한 주소/전화/대표자 정보가
     * 같은 줄로 잘못 합쳐지는 경우가 실제 테스트(하나로마트 영수증)에서 발견됨.
     * ("주소: 대표: 서울시 장순석 강서구 금낭화로 전화: 287-10 02-2669-6000" 처럼
     *  사업자번호 바로 위 줄이 상호명이 아니라 노이즈 블록이 되어버림)
     * 이런 경우 바로 포기하지 않고, 노이즈가 아닌 줄이 나올 때까지 최대
     * MERCHANT_LOOKBACK_LIMIT줄까지 더 위로 올라가며 찾는다. (실제로 상호명은
     * 노이즈 블록보다 한두 줄 더 위, 영수증 맨 위쪽에 있는 경우가 많았음)
     * 그래도 못 찾으면 틀린 값을 채우는 것보다 null을 반환하는 게 안전하다는 판단.
     * 사업자번호 자체를 못 찾으면 첫 줄로 폴백.
     */
    private String extractMerchantName(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (BUSINESS_NO_PATTERN.matcher(lines.get(i)).find()) {
                for (int back = 1; back <= MERCHANT_LOOKBACK_LIMIT && i - back >= 0; back++) {
                    String candidate = lines.get(i - back).trim();
                    if (!MERCHANT_NOISE_PATTERN.matcher(candidate).find()) {
                        return candidate;
                    }
                }
                return null;
            }
        }
        return lines.isEmpty() ? null : lines.get(0).trim();
    }

    private String extractDate(List<String> lines) {
        for (String rawLine : lines) {
            // 사업자등록번호(XXX-XX-XXXXX)가 날짜 패턴과 부분적으로 겹쳐 오인식되는 걸 막기 위해
            // 줄 전체를 버리지 않고, 사업자번호 부분만 제거한 뒤 날짜를 탐색한다.
            // (줄 재조립 과정에서 날짜가 사업자번호와 같은 줄로 잘못 합쳐지는 영수증이 실제로 있었음 -
            //  이 경우 줄 전체를 skip하면 정작 그 안에 있는 진짜 날짜까지 같이 버려지는 문제가 있었음)
            String line = BUSINESS_NO_PATTERN.matcher(rawLine).replaceAll(" ");

            Matcher matcher = DATE_PATTERN.matcher(line);
            while (matcher.find()) {
                String year = matcher.group(1);
                if (year.length() == 2) {
                    year = "20" + year; // 2자리 연도 보정 (2000년대 가정)
                }
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                if (isPlausibleDate(month, day)) {
                    return "%s-%s-%s".formatted(year, zeroPad(matcher.group(2)), zeroPad(matcher.group(3)));
                }
                // 범위를 벗어난 매치(오인식)는 버리고 같은 줄에서 다음 후보를 계속 탐색
            }
        }
        return null;
    }

    // 날짜 검증: 달력상 존재할 수 없는 월/일이면 오인식으로 간주 (extractAmount의 isPlausibleAmount와 동일한 취지)
    private boolean isPlausibleDate(int month, int day) {
        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    private Long extractAmount(List<String> lines) {
        for (String line : lines) {
            if (TABLE_HEADER_PATTERN.matcher(line).find()) {
                continue; // "품명 단가 수량 금액" 같은 열 제목 줄은 건너뜀
            }
            // 사업자번호(XXX-XX-XXXXX)가 섞인 줄은 건너뜀.
            // 하이픈으로 끊긴 숫자 조각(예: 마지막 5자리 "67890")이
            // AMOUNT_NUMBER_PATTERN([\d,]{4,})에 걸려 총액으로 오인되는 걸 방지.
            // (실제로 "총구매액"이 있어야 할 자리에 사업자번호 조각이 나온 버그의 원인)
            if (BUSINESS_NO_PATTERN.matcher(line).find()) {
                continue;
            }
            if (countNumberGroups(line) > MAX_NUMBERS_PER_LINE) {
                continue; // 상품이 여러 개 뭉쳐진 표 줄일 가능성이 높아 신뢰할 수 없음
            }
            if (AMOUNT_KEYWORD_PATTERN.matcher(line).find()) {
                Matcher matcher = AMOUNT_NUMBER_PATTERN.matcher(line);
                if (matcher.find()) {
                    long value = Long.parseLong(matcher.group().replace(",", ""));
                    if (isPlausibleAmount(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    // 금액 검증: 0원 이하이거나 비현실적으로 큰 값이면 오인식으로 간주
    private boolean isPlausibleAmount(long value) {
        return value > 0 && value <= 10_000_000;
    }

    // 줄 안에 [\d,]{4,} 패턴이 몇 번 나오는지 카운트 (다중 상품 표 줄 판별용)
    private int countNumberGroups(String line) {
        Matcher matcher = AMOUNT_NUMBER_PATTERN.matcher(line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String zeroPad(String number) {
        return number.length() == 1 ? "0" + number : number;
    }
}
