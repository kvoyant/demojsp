package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceApiVerifyRequestDto {
    private String requestNo;        // 금융사 트랜잭션 번호
    private String resSeq;           // NICE 발행 트랜잭션 번호
    private String authToken;        // NICE 인증 토큰
    private String otpCode;          // 고객 입력 OTP 번호 (6자리)
}
