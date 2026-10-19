package com.example.demojsp.common.acctverify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 1원 계좌인증 요청·결과 1행 (TB_CO_ACCT_VERIFY) - 서버 내부 · ADMIN 이력용 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvReqDto {

    public static final String SENT = "SENT";
    public static final String VERIFIED = "VERIFIED";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    private String reqNo;
    private String bizCd;
    private String channel;
    private String custId;
    private String sessKey;
    private String custNm;
    private String bankCd;
    private String bankNm;
    private String acctNo;
    private String acctMask;
    private String holderNm;
    private String source;        // PREFILL / INPUT
    private String prefillId;
    private String modifiedYn;
    private String providerTx;
    private String codeHash;
    private Integer tryCnt;
    private LocalDateTime expireAt;
    private String status;
    private String resultCd;
    private String clientIp;
    private LocalDateTime reqAt;
    private LocalDateTime doneAt;

    /** 조회용 */
    private String bizNm;
    private String screenType;
}
