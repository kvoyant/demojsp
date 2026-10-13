package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1원 계좌인증 감사 이력 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCertHistDto {
    private String reqTxSeq;
    private String memberId;
    private String custNm;
    private String bankCd;
    private String bankNm;
    private String accountNo;
    private String accountHolderNm;
    private String authCode;
    private String printText;
    private String status;           // REQUESTED, VERIFIED, FAILED, EXPIRED
    private String purpose;          // LOAN_APPLY, COMMON
    private String loanNo;
    private String isModified;       // Y/N
    private Integer failCnt;
    private LocalDateTime expireDt;
    private LocalDateTime verifyDt;
    private String clientIp;
    private String userAgent;
    private LocalDateTime regDt;
}
