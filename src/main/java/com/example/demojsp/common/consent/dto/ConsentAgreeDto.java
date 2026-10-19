package com.example.demojsp.common.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 약관동의 내역 헤더 (TB_CO_TERMS_AGREE) + 상세 목록 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // 비어 있는 항목은 응답에서 생략
public class ConsentAgreeDto {

    private Long agreeNo;
    private String bizCd;
    private String custId;
    private String sessKey;
    private String channel;
    private String clientIp;
    private LocalDateTime agreeAt;

    /** 조회용 */
    private String bizNm;
    private Integer agreeCnt;     // 동의 Y 건수
    private Integer totalCnt;     // 전체 약관 건수
    private List<ConsentAgreeItemDto> items = new ArrayList<>();
}
