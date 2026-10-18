package com.example.demojsp.common.terms.domain;

import java.io.Serializable;

/**
 * 약관 목록용 요약 (전문 미포함 → 목록 응답을 가볍게, 전문은 상세보기 시 별도 조회).
 * 화면은 동의 요청 시 여기의 {@code version} 을 그대로 돌려보내야 한다(개정 중 동의 방지).
 */
public record TermsSummary(
        String termsCode,
        String title,
        boolean required,
        String version,
        int sortOrder,
        String pdfUrl,
        String contentHash
) implements Serializable {
}
