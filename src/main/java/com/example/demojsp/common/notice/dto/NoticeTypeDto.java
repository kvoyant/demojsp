package com.example.demojsp.common.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 공지 유형 (TB_CO_NOTICE_TYPE) - AS-IS 업무공지/서비스공지/시스템점검/기타, TO-BE 는 ADMIN 에서 추가·변경 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeTypeDto {

    private String typeCd;
    private String typeNm;
    private String badgeColor;
    private Integer sortOrd;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;

    /** 조회용: 이 유형을 쓰는 공지 수 */
    private Integer noticeCnt;
}
