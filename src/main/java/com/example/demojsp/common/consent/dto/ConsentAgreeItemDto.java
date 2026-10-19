package com.example.demojsp.common.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 약관별 동의 1건.
 * <ul>
 *   <li>요청: termsCd, termsVer(화면이 본 버전), agreeYn, viewYn</li>
 *   <li>저장/조회: TB_CO_TERMS_AGREE_DTL (+ termsNm 은 조회용)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // 비어 있는 항목은 응답에서 생략
public class ConsentAgreeItemDto {

    private Long agreeNo;
    private String termsCd;
    private Integer termsVer;
    private String cnHash;
    private String reqYn;
    private String agreeYn;
    private String viewYn;

    /** 조회용 */
    private String termsNm;

    public ConsentAgreeItemDto(String termsCd, Integer termsVer, String agreeYn) {
        this.termsCd = termsCd;
        this.termsVer = termsVer;
        this.agreeYn = agreeYn;
    }
}
