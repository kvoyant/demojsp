package com.example.demojsp.common.terms.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 약관 동의 완료 결과 - 세션에 업무코드별로 저장되고, 업무 API 가 {@code TermsSessionManager.require()} 로 꺼내 쓴다.
 *
 * @param agreementId     동의 거래번호 (업무 데이터에 함께 저장하면 이력과 연결된다)
 * @param bizCode         업무코드
 * @param agreedCodes     동의한 약관코드 (필수 + 동의한 선택)
 * @param declinedCodes   미동의 선택 약관코드
 * @param versions        약관코드=버전 목록 (예: "SVC_USE=1.0")
 * @param agreedAt        처리 일시 (화면 표시용)
 * @param agreedInstant   처리 시각 (세션 만료 판단용)
 */
public record AgreeResult(
        String agreementId,
        String bizCode,
        List<String> agreedCodes,
        List<String> declinedCodes,
        List<String> versions,
        String agreedAt,
        Instant agreedInstant
) implements Serializable {

    /** 선택약관 동의 여부 (예: 마케팅 수신 동의 시에만 업무 처리) */
    public boolean isAgreed(String termsCode) {
        return agreedCodes.contains(termsCode);
    }
}
