package com.example.demojsp.common.platformlink.dto;

import lombok.Data;

/** 제휴사별 처리 현황 (ADMIN) */
@Data
public class LnkStatDto {
    private String partnerCd;
    private String partnerNm;
    private Integer timeoutMs;
    private Integer total;
    private Integer approved;
    private Integer rejected;
    private Integer failed;
    private Integer timeouts;
    private Integer avgMs;
    private Integer maxMs;
    /** 전체 처리시간이 제휴사 TIMEOUT_MS 를 넘긴 건 (제휴사 쪽에서는 응답 못 받은 것) */
    private Integer overTimeout;
}
