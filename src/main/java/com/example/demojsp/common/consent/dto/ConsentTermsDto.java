package com.example.demojsp.common.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 약관 1건 (TB_CO_TERMS 버전 행 + 업무별 약관 TB_CO_BIZ_TERMS 의 필수여부·순서).
 *
 * <p>목록 API 는 전문(termsCn)을 비워서 내려주고, 상세 API 에서만 채운다 (응답 크기 ↓).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // 비어 있는 항목은 응답에서 생략
public class ConsentTermsDto {

    // ---- 업무별 약관 (TB_CO_BIZ_TERMS)
    private String bizCd;
    private String reqYn;        // Y 필수 / N 선택
    private Integer sortOrd;
    private String mapUseYn;     // 업무별 약관 사용여부

    // ---- 약관 (TB_CO_TERMS)
    private String termsCd;
    private Integer termsVer;
    private String termsNm;
    private String termsCn;
    private String pdfUrl;
    private String cnHash;
    private LocalDate effectDt;
    private String useYn;
    private LocalDateTime regAt;
    private String regBy;

    /** ADMIN 약관 목록용: 버전 수 / 현재 시행 버전 */
    private Integer verCnt;
    private Integer currentVer;
}
