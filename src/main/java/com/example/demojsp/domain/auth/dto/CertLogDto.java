package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertLogDto {
    private Long certSeq;
    private String reqTxSeq;
    private String channelType;   // APP, PC_WEB, MO_WEB
    private String authMethod;    // SMS_NO_CARRIER, MO_DEVICE_AUTH
    private String custNm;
    private String birthDate;
    private String cellNo;
    private String ciVal;
    private String authToken;
    private String authStatus;    // REQUESTED, VERIFIED, RISK_BLOCKED, EXPIRED, FAILED
    private String errCode;
    private String errMsg;
    private String clientIp;
    private String userAgent;
    private LocalDateTime reqDt;
    private LocalDateTime resDt;
}
