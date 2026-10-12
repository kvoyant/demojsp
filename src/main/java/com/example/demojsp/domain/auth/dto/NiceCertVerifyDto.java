package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertVerifyDto {
    private String reqTxSeq;       // 요청 트랜잭션 번호
    private String otpCode;        // SMS 인증번호 6자리
    private String authToken;      // 발급된 인증 토큰
}
