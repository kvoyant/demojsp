package com.example.demojsp.common.consent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 약관동의 결과 - 동의 API 응답이자, 업무 서버가 {@code consentService.checkAgreed(...)} 로 받는 값.
 *
 * <pre>
 *  member.setTermsAgreeNo(result.getAgreeNo());                       // 업무 데이터 ↔ 동의내역 연결
 *  member.setMarketingYn(result.isAgreed("MARKETING_USE") ? "Y" : "N"); // 선택약관 반영
 * </pre>
 */
@Getter
public class ConsentResult {

    private final Long agreeNo;
    private final String bizCd;
    private final LocalDateTime agreeAt;
    private final List<String> agreedTerms;     // 동의한 약관코드
    private final List<String> declinedTerms;   // 동의하지 않은 (선택) 약관코드

    public ConsentResult(Long agreeNo, String bizCd, LocalDateTime agreeAt, List<String> agreedTerms, List<String> declinedTerms) {
        this.agreeNo = agreeNo;
        this.bizCd = bizCd;
        this.agreeAt = agreeAt;
        this.agreedTerms = List.copyOf(agreedTerms);
        this.declinedTerms = List.copyOf(declinedTerms);
    }

    @JsonIgnore
    public boolean isAgreed(String termsCd) {
        return agreedTerms.contains(termsCd);
    }
}
