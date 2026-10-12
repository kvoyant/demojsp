package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceApiOtpResponseDto {
    private boolean success;
    private String resultCode;       // 0000: 성공, 기타: NICE 에러코드
    private String resultMessage;
    private String requestNo;        // 금융사 트랜잭션 번호
    private String resSeq;           // NICE 발행 트랜잭션 시퀀스
    private String authToken;        // NICE 검증용 인증 토큰
    private int expiresInSeconds;    // OTP 유효시간 (180초)
    private String detectedCarrier;  // NICE가 자동 식별한 통신사 (SKT, KT, LGT, MVNO 등)
    private String otpCodeForDebug;  // 개발/테스트 시뮬레이션용 OTP (운영망에서는 null)
}
