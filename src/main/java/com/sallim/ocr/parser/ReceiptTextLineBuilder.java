package com.sallim.ocr.parser;

import com.sallim.ocr.dto.ClovaOcrResponse.Field;
import com.sallim.ocr.dto.ClovaOcrResponse.Vertex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CLOVA OCR 응답의 텍스트 조각(field)들을 한 줄(line) 단위로 재조립하는 클래스.
 *
 * CLOVA OCR은 텍스트를 글자 조각 단위로 좌표와 함께 반환하기 때문에, 어떤 조각들이
 * 원래 같은 줄이었는지는 우리가 직접 판단해서 다시 이어붙여야 한다.
 * (예: "GS25 강남점" → "GS25", "강남점" 두 조각으로 따로 옴)
 *
 * [문제] 단순 top-y 오차 허용(epsilon) 방식의 한계
 * 처음에는 "두 필드의 top-y 차이가 일정 값 이내면 같은 줄"로 판단했으나,
 * 실제 응답에서는 같은 줄인데도 top-y가 15~20px씩 차이나는 경우가 있었다.
 * (예: "금액" top=876 vs "650,000원" top=858 → 18px 차이)
 * epsilon을 15px로 잡으면 이런 케이스가 다른 줄로 잘못 쪼개진다.
 * 픽셀 차이는 절대값이라, 글자 크기·이미지 해상도가 바뀌면 기준 자체가 흔들리는 한계도 있다.
 *
 * [해결] 겹침(overlap) 비율 기반 판정
 * top-y 한 점을 비교하는 대신, 각 필드가 차지하는 세로 구간(top~bottom) 전체를 비교한다.
 * 두 구간이 겹치는 길이를 필드 높이로 나눈 비율이 30% 이상이면 같은 줄로 판정한다.
 * 필드 크기에 비례한 상대값이라, 해상도나 글자 크기가 달라져도 기준이 안정적으로 유지된다.
 * (실무 OCR 라인 재구성에서 흔히 쓰이는 방식)
 */
@Component
public class ReceiptTextLineBuilder {

    private static final double OVERLAP_RATIO_THRESHOLD = 0.3; // 30% 이상 겹치면 같은 줄로 인정

    public List<String> buildLines(List<Field> fields) {
        List<Field> sortedByTop = fields.stream()
                .sorted(Comparator.comparingDouble(this::topY))
                .toList();

        List<Line> lines = new ArrayList<>();
        for (Field field : sortedByTop) {
            Line matchedLine = lines.stream()
                    .filter(line -> overlaps(line, field))
                    .findFirst()
                    .orElse(null);

            if (matchedLine != null) {
                matchedLine.add(field);
            } else {
                lines.add(new Line(field, this));
            }
        }

        return lines.stream()
                .map(Line::toText)
                .toList();
    }

    private boolean overlaps(Line line, Field field) {
        double fieldTop = topY(field);
        double fieldBottom = bottomY(field);
        double overlap = Math.min(line.bottom, fieldBottom) - Math.max(line.top, fieldTop);
        double fieldHeight = fieldBottom - fieldTop;
        if (fieldHeight <= 0) {
            return false;
        }
        return overlap > 0 && (overlap / fieldHeight) > OVERLAP_RATIO_THRESHOLD;
    }

    double topY(Field field) {
        return field.boundingPoly().vertices().stream().mapToDouble(Vertex::y).min().orElse(0);
    }

    double bottomY(Field field) {
        return field.boundingPoly().vertices().stream().mapToDouble(Vertex::y).max().orElse(0);
    }

    double leftX(Field field) {
        return field.boundingPoly().vertices().stream().mapToDouble(Vertex::x).min().orElse(0);
    }

    /**
     * 한 줄을 구성하는 필드들을 모아두는 내부 헬퍼.
     * top/bottom은 이 줄에 지금까지 들어온 필드들 전체를 아우르는 범위로 계속 확장됨.
     */
    private static class Line {
        private final ReceiptTextLineBuilder outer;
        private final List<Field> fields = new ArrayList<>();
        double top;
        double bottom;

        Line(Field first, ReceiptTextLineBuilder outer) {
            this.outer = outer;
            add(first);
        }

        void add(Field field) {
            double fieldTop = outer.topY(field);
            double fieldBottom = outer.bottomY(field);
            top = fields.isEmpty() ? fieldTop : Math.min(top, fieldTop);
            bottom = fields.isEmpty() ? fieldBottom : Math.max(bottom, fieldBottom);
            fields.add(field);
        }

        String toText() {
            return fields.stream()
                    .sorted(Comparator.comparingDouble(outer::leftX))
                    .map(Field::inferText)
                    .collect(Collectors.joining(" "));
        }
    }
}
