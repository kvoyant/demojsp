package com.example.demojsp.common.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 업무코드 (TB_CO_TERMS_BIZ) */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // 비어 있는 항목은 응답에서 생략
public class ConsentBizDto {

    private String bizCd;
    private String bizNm;
    private String bizDesc;
    private String useYn;
    private LocalDateTime regAt;
    private String regBy;
    private LocalDateTime updAt;
    private String updBy;

    /** 조회용: 이 업무에 연결된(사용중) 필수 / 선택 약관 수 */
    private Integer reqCnt;
    private Integer optCnt;
}
