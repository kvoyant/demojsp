package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1원 인증번호 검증 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneWonVerifyResDto {
    private boolean success;
    private String reqTxSeq;            // 인증 트랜잭션 번호
    private String authConfirmToken;    // 본인확인 완료 검증 토큰 (UUID)
    private String bankCd;              // 은행코드
    private String bankNm;              // 은행명
    private String accountNoMasked;     // 마스킹 계좌번호
    private String accountHolderNm;     // 예금주명
    private LocalDateTime verifyDt;     // 인증 완료일시
    private String message;
}
