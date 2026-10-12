package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private boolean success;
    private boolean requiresTwoFactor;   // 상담사/관리자 2차 인증 필요 여부
    private String twoFactorTxSeq;        // 2차 인증 트랜잭션 번호
    private String maskedPhone;           // 2차 인증번호 발송 전화번호
    private String testOtpCode;           // 테스트용 2차 OTP 코드 (화면 편의용)
    private String message;
    private UserSessionDto sessionUser;   // 로그인 완료 시 사용자 정보
    private String redirectUrl;           // 이동할 페이지 URL
}
