package com.example.demojsp.common.terms.domain;

/**
 * [ADMIN] 약관 등록/개정 요청. 같은 (bizCode, termsCode) 로 새 version 을 등록하면 그 버전이 현재 약관이 된다.
 */
public record TermsRegisterRequest(
        String bizCode,
        String termsCode,
        String title,
        Boolean required,
        String version,
        Integer sortOrder,
        String content,
        String pdfUrl
) {
}
