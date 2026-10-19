package com.example.demojsp.common.niceid.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 본인인증 요청·결과 1행 (TB_CO_IDV_REQ) - 서버 내부 · ADMIN 이력용 (사용자 화면에는 IdvResult 로 필요한 값만 내보낸다) */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdvReqDto {

    public static final String SMS = "SMS";
    public static final String MO = "MO";

    public static final String REQUESTED = "REQUESTED";
    public static final String VERIFIED = "VERIFIED";
    public static final String BLOCKED = "BLOCKED";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    private String reqNo;
    private String purposeCd;
    private String method;
    private String channel;
    private Long agreeNo;
    private String custId;
    private String sessKey;
    private String custNm;
    private String birthDt;
    private String phoneMask;
    private String providerTx;
    private String moCode;
    private Integer tryCnt;
    private LocalDateTime expireAt;
    private String status;
    private String resultCd;
    private String ciVal;
    private String diVal;
    private String cellNo;
    private String gender;
    private String nationalCd;
    private Integer age;
    private String clientIp;
    private LocalDateTime reqAt;
    private LocalDateTime doneAt;

    /** 조회용 */
    private String purposeNm;
}
