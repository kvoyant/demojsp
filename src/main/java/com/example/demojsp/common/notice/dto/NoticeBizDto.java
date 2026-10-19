package com.example.demojsp.common.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 업무 구분 (TB_CO_NOTICE_BIZ) - "업무별 공지" 의 업무 목록 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeBizDto {

    private String bizCd;
    private String bizNm;
    private Integer sortOrd;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;
}
