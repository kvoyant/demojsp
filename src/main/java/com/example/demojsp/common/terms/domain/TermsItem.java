package com.example.demojsp.common.terms.domain;

import java.io.Serializable;

/**
 * 약관 1건(특정 버전). ADMIN 에서 등록한 약관 원장에 해당한다.
 *
 * <p>같은 (bizCode, termsCode) 에 여러 버전이 존재할 수 있고, 가장 마지막에 등록(시행)된 버전이 "현재 약관" 이다.
 * 과거 버전은 동의 이력(증적) 조회를 위해 삭제하지 않는다.
 *
 * @param bizCode      업무코드 (예: SIGN_UP, LOAN_APPLY, IDENTITY_NICE_MOBILE)
 * @param termsCode    약관코드 (업무코드 내 유일)
 * @param title        약관명
 * @param required     필수 동의 여부
 * @param version      약관 버전
 * @param sortOrder    표시 순서
 * @param content      약관 전문
 * @param pdfUrl       PDF 원문 URL (없으면 null) - CF-NA-03 PDF/약관 Viewer 로 연결
 * @param contentHash  전문 SHA-256 (동의 이력에 함께 저장 → "어떤 문구에 동의했는지" 증적)
 * @param registeredAt 등록(시행) 일시 (yyyy-MM-dd HH:mm:ss)
 * @param registeredBy 등록자 (seed 는 SYSTEM)
 */
public record TermsItem(
        String bizCode,
        String termsCode,
        String title,
        boolean required,
        String version,
        int sortOrder,
        String content,
        String pdfUrl,
        String contentHash,
        String registeredAt,
        String registeredBy
) implements Serializable {

    /** 목록 화면용 요약 (전문 제외) */
    public TermsSummary toSummary() {
        return new TermsSummary(termsCode, title, required, version, sortOrder, pdfUrl, contentHash);
    }
}
