package com.example.demojsp.common.platformlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** TB_LNK_LIMIT_REQ - 한도조회 이력 1건 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LnkReqDto {
    public static final String RECEIVED = "RECEIVED";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    private String lnkReqNo;
    private String partnerCd;
    private String partnerReqNo;
    private String cssProduct;
    private String ciHash;
    private String custNmMask;
    private Long annualIncome;
    private String cssEngine;
    private String status;
    private String resultCd;
    private String outerCd;
    private Long limitAmt;
    private Integer rateBp;
    private String cssTxNo;
    private Integer cssMs;
    private Integer totalMs;
    private String shadowResult;
    private String resBody;
    private String clientIp;
    private LocalDateTime reqAt;
    private LocalDateTime resAt;

    // 조회 전용 (JOIN)
    private String partnerNm;
    private Integer timeoutMs;
}
