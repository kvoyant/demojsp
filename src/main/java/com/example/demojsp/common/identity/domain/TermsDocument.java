package com.example.demojsp.common.identity.domain;

/**
 * 인증수단별 약관 1건.
 *
 * @param code       약관 코드 (동의 여부 검증 시 사용)
 * @param title      약관명 (화면 표시)
 * @param required   필수 동의 여부
 * @param version    약관 버전 (동의 이력 저장 시 함께 기록 권장)
 * @param contentUrl 약관 상세보기 URL (화면에서 레이어/새창으로 표시)
 */
public record TermsDocument(
        String code,
        String title,
        boolean required,
        String version,
        String contentUrl
) {
}
