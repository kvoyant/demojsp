package com.example.demojsp.domain.login.service;

import com.example.demojsp.domain.login.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface LoginService {

    /**
     * ID / 비밀번호 기반 1차 로그인
     * - 개인: 즉시 로그인 완료 (세션 발급)
     * - 상담사/관리자: 1차 검증 후 2차 인증(SMS OTP) 발송
     */
    LoginResponseDto login(LoginRequestDto reqDto, HttpServletRequest request);

    /**
     * 상담사 / 관리자 2차 인증(OTP) 검증 및 최종 로그인 완료
     */
    LoginResponseDto verifyTwoFactor(TwoFactorVerifyDto verifyDto, HttpServletRequest request);

    /**
     * NICE ID 휴대폰 본인인증(CI) 연계 개인고객 간편 로그인
     */
    LoginResponseDto loginWithNiceCi(NiceLoginRequestDto niceDto, HttpServletRequest request);

    /**
     * 로그아웃 처리 (세션 만료 및 동시접속 해제)
     */
    void logout(HttpServletRequest request);

    /**
     * 계정 잠금 해제 (본인인증 통과 또는 관리자 권한)
     */
    void unlockAccount(String memberId);
}
