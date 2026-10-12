package com.example.demojsp.domain.login;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.domain.login.dto.*;
import com.example.demojsp.domain.login.mapper.MemberMapper;
import com.example.demojsp.domain.login.service.ConcurrentSessionManager;
import com.example.demojsp.domain.login.service.LoginService;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoginServiceTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ConcurrentSessionManager concurrentSessionManager;

    @Test
    @DisplayName("1. 개인고객 정상 로그인 - 2FA 불필요, 30분 세션 부여, 실패횟수 초기화")
    void testIndividualLogin_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginRequestDto req = LoginRequestDto.builder()
                .memberId("customer01")
                .password("Password123!")
                .memberType("INDIVIDUAL")
                .build();

        LoginResponseDto response = loginService.login(req, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isRequiresTwoFactor()).isFalse(); // 개인은 2차 인증 불필요
        assertThat(response.getSessionUser()).isNotNull();
        assertThat(response.getSessionUser().getMemberId()).isEqualTo("customer01");
        assertThat(response.getSessionUser().getRoleCd()).isEqualTo("ROLE_CUSTOMER");
        assertThat(response.getSessionUser().getTimeoutMinutes()).isEqualTo(30); // 30분 세션

        // 세션 내 사용자 등록 검증
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertThat(session).isNotNull();
        UserSessionDto sessionUser = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        assertThat(sessionUser).isNotNull();
        assertThat(sessionUser.getMemberNm()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("2. 비밀번호 불일치 시 예외 발생 및 5회 잠금 정책 검증")
    void testLoginPasswordFailAndLock() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1) 비밀번호 틀림 -> LOGIN_PASSWORD_MISMATCH
        LoginRequestDto wrongPwReq = LoginRequestDto.builder()
                .memberId("customer01")
                .password("WrongPassword999!")
                .memberType("INDIVIDUAL")
                .build();

        assertThatThrownBy(() -> loginService.login(wrongPwReq, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_PASSWORD_MISMATCH);

        // 2) 이미 5회 실패로 잠긴 계정(locked01) 시도 -> LOGIN_ACCOUNT_LOCKED
        LoginRequestDto lockedReq = LoginRequestDto.builder()
                .memberId("locked01")
                .password("Password123!")
                .memberType("INDIVIDUAL")
                .build();

        assertThatThrownBy(() -> loginService.login(lockedReq, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("3. 대출상담사 로그인 - 전자금융감독규정에 따른 2차 인증(SMS OTP) 필수 및 검증 통과")
    void testCounselorLogin_TwoFactorAuth() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1) 1차 로그인 시도 -> 2차 인증 요구 (SMS OTP 발송)
        LoginRequestDto req = LoginRequestDto.builder()
                .memberId("counselor01")
                .password("Password123!")
                .memberType("COUNSELOR")
                .build();

        LoginResponseDto step1Res = loginService.login(req, request);

        assertThat(step1Res.isSuccess()).isTrue();
        assertThat(step1Res.isRequiresTwoFactor()).isTrue();
        assertThat(step1Res.getTwoFactorTxSeq()).isNotNull();
        assertThat(step1Res.getMaskedPhone()).contains("010-****-");
        assertThat(step1Res.getTestOtpCode()).hasSize(6);

        // 2) 잘못된 2차 OTP 입력 시 오류
        TwoFactorVerifyDto wrongOtp = TwoFactorVerifyDto.builder()
                .twoFactorTxSeq(step1Res.getTwoFactorTxSeq())
                .otpCode("000000")
                .build();

        assertThatThrownBy(() -> loginService.verifyTwoFactor(wrongOtp, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_TWO_FACTOR_MISMATCH);

        // 3) 정상 2차 OTP 입력 시 최종 로그인 완료 (상담사 15분 세션)
        TwoFactorVerifyDto validOtp = TwoFactorVerifyDto.builder()
                .twoFactorTxSeq(step1Res.getTwoFactorTxSeq())
                .otpCode(step1Res.getTestOtpCode())
                .build();

        LoginResponseDto finalRes = loginService.verifyTwoFactor(validOtp, request);

        assertThat(finalRes.isSuccess()).isTrue();
        assertThat(finalRes.getSessionUser()).isNotNull();
        assertThat(finalRes.getSessionUser().getRoleCd()).isEqualTo("ROLE_COUNSELOR");
        assertThat(finalRes.getSessionUser().getTimeoutMinutes()).isEqualTo(15); // 상담사 15분 세션
    }

    @Test
    @DisplayName("4. 관리자 로그인 - 2차 인증 통과 후 심사역/관리자 권한 및 15분 세션 발급")
    void testAdminLogin_TwoFactorAuth() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        LoginRequestDto req = LoginRequestDto.builder()
                .memberId("admin01")
                .password("Password123!")
                .memberType("ADMIN")
                .build();

        LoginResponseDto step1Res = loginService.login(req, request);
        assertThat(step1Res.isRequiresTwoFactor()).isTrue();

        TwoFactorVerifyDto validOtp = TwoFactorVerifyDto.builder()
                .twoFactorTxSeq(step1Res.getTwoFactorTxSeq())
                .otpCode(step1Res.getTestOtpCode())
                .build();

        LoginResponseDto finalRes = loginService.verifyTwoFactor(validOtp, request);
        assertThat(finalRes.getSessionUser().getRoleCd()).isEqualTo("ROLE_ADMIN");
        assertThat(finalRes.getSessionUser().getTimeoutMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("5. NICE ID 휴대폰 본인인증(CI) 연계 간편 로그인 및 잠금 해제")
    void testNiceCiLogin_And_Unlock() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // locked01의 CI로 본인인증 간편 로그인 시도 -> 잠금 자동 해제 및 로그인
        MemberDto lockedMember = memberMapper.selectMemberById("locked01");
        assertThat(lockedMember.getLockYn()).isEqualTo("Y");

        NiceLoginRequestDto niceReq = NiceLoginRequestDto.builder()
                .ciVal(lockedMember.getCiVal())
                .custNm("잠금고객")
                .cellNo("01099998888")
                .build();

        LoginResponseDto res = loginService.loginWithNiceCi(niceReq, request);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getSessionUser().getMemberId()).isEqualTo("locked01");

        // DB 상 잠금 상태가 해제되었는지 검증
        MemberDto updatedMember = memberMapper.selectMemberById("locked01");
        assertThat(updatedMember.getLockYn()).isEqualTo("N");
        assertThat(updatedMember.getLoginFailCnt()).isEqualTo(0);
    }

    @Test
    @DisplayName("6. 중복 로그인 제어 - 신규 로그인 시 기존 세션 무효화")
    void testConcurrentSessionControl() {
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletRequest req2 = new MockHttpServletRequest();

        LoginRequestDto loginDto = LoginRequestDto.builder()
                .memberId("customer01")
                .password("Password123!")
                .memberType("INDIVIDUAL")
                .build();

        // 1번 브라우저 로그인
        loginService.login(loginDto, req1);
        MockHttpSession session1 = (MockHttpSession) req1.getSession(false);
        assertThat(session1.isInvalid()).isFalse();

        // 2번 브라우저에서 동일 아이디로 로그인
        loginService.login(loginDto, req2);
        MockHttpSession session2 = (MockHttpSession) req2.getSession(false);
        assertThat(session2.isInvalid()).isFalse();

        // 1번 세션은 무효화(invalidate) 되었는지 검증
        assertThat(session1.isInvalid()).isTrue();
    }
}
