package com.example.demojsp.domain.login.controller;

import com.example.demojsp.common.annotation.RequireRole;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.domain.login.dto.*;
import com.example.demojsp.domain.login.service.LoginService;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================================
 * [로그인 및 세션 관리 REST API 컨트롤러 (LoginApiController)]
 * - 1차 로그인 (개인/상담사/관리자)
 * - 2차 인증(2FA SMS OTP) 검증
 * - NICE ID 본인확인(CI) 간편 로그인
 * - 세션 조회, 세션 연장, 로그아웃, 잠금 해제
 * =========================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginApiController {

    private final LoginService loginService;

    /**
     * 1. 기본 1차 로그인
     * - 개인: 바로 로그인 완료
     * - 상담사/관리자: 1차 일치 시 2FA SMS 발송 후 requiresTwoFactor: true 반환
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@RequestBody LoginRequestDto reqDto, HttpServletRequest request) {
        LoginResponseDto responseDto = loginService.login(reqDto, request);
        return ApiResponse.success(responseDto);
    }

    /**
     * 2. 상담사 / 관리자 2차 인증(SMS OTP) 검증 및 최종 로그인
     */
    @PostMapping("/login/2fa")
    public ApiResponse<LoginResponseDto> verifyTwoFactor(@RequestBody TwoFactorVerifyDto verifyDto, HttpServletRequest request) {
        LoginResponseDto responseDto = loginService.verifyTwoFactor(verifyDto, request);
        return ApiResponse.success(responseDto);
    }

    /**
     * 3. NICE ID 휴대폰 본인확인(CI) 연계 간편 로그인
     */
    @PostMapping("/login/nice-ci")
    public ApiResponse<LoginResponseDto> loginWithNiceCi(@RequestBody NiceLoginRequestDto niceDto, HttpServletRequest request) {
        LoginResponseDto responseDto = loginService.loginWithNiceCi(niceDto, request);
        return ApiResponse.success(responseDto);
    }

    /**
     * 4. 로그아웃 API
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        loginService.logout(request);
        return ApiResponse.success(null);
    }

    /**
     * 5. 현재 로그인 세션 정보 조회
     */
    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> getSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ApiResponse.error(ErrorCode.SESSION_EXPIRED);
        }

        UserSessionDto user = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        if (user == null) {
            return ApiResponse.error(ErrorCode.SESSION_EXPIRED);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("maxInactiveInterval", session.getMaxInactiveInterval());
        result.put("lastAccessedTime", session.getLastAccessedTime());

        return ApiResponse.success(result);
    }

    /**
     * 6. 세션 연장 (사용자가 세션 만료 경고 시 '연장' 클릭)
     */
    @PostMapping("/session/extend")
    public ApiResponse<Map<String, Object>> extendSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ApiResponse.error(ErrorCode.SESSION_EXPIRED);
        }

        UserSessionDto user = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        if (user == null) {
            return ApiResponse.error(ErrorCode.SESSION_EXPIRED);
        }

        // 기존 설정된 세션 타임아웃(개인 30분, 상담사/관리자 15분)으로 재설정
        int timeoutMinutes = user.getTimeoutMinutes() > 0 ? user.getTimeoutMinutes() : 30;
        session.setMaxInactiveInterval(timeoutMinutes * 60);

        Map<String, Object> result = new HashMap<>();
        result.put("extendedMinutes", timeoutMinutes);
        result.put("maxInactiveInterval", session.getMaxInactiveInterval());
        result.put("message", "로그인 세션이 " + timeoutMinutes + "분 연장되었습니다.");

        return ApiResponse.success(result);
    }

    /**
     * 7. 계정 잠금 해제 (관리자 권한 필요)
     */
    @PostMapping("/unlock")
    @RequireRole({"ROLE_ADMIN"})
    public ApiResponse<String> unlockAccount(@RequestParam String memberId) {
        loginService.unlockAccount(memberId);
        return ApiResponse.success("계정 [" + memberId + "]의 잠금이 성공적으로 해제되었습니다.");
    }
}
