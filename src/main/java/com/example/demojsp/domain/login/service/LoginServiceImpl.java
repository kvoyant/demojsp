package com.example.demojsp.domain.login.service;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.common.util.MaskingUtil;
import com.example.demojsp.domain.auth.client.SmsGatewayClient;
import com.example.demojsp.domain.auth.client.dto.SmsSendRequestDto;
import com.example.demojsp.domain.login.dto.*;
import com.example.demojsp.domain.login.mapper.MemberMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================================
 * [저축은행 로그인 정책(개인, 상담사, 관리자, 권한) 서비스 구현체]
 * 
 * 1. 전자금융감독규정 준수 정책:
 *    • 개인고객: 30분 세션 타임아웃, 5회 비밀번호 오류 시 계정 잠금, NICE CI 간편 로그인 지원
 *    • 상담사/모집인: 15분 세션 타임아웃, 2차 추가인증(SMS OTP) 필수, 중복 로그인 차단
 *    • 심사역/관리자: 15분 세션 타임아웃, 2차 추가인증(SMS OTP) 필수, 접근권한(RBAC) 엄격 통제
 * 
 * 2. 감사 로그:
 *    • 모든 로그인 성공/실패 시도에 대해 IP, User-Agent, 실패 사유를 TB_LOGIN_LOG에 영속화
 * =========================================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    public static final String SESSION_USER_KEY = "LOGIN_USER";

    private final MemberMapper memberMapper;
    private final SmsGatewayClient smsGatewayClient;
    private final ConcurrentSessionManager concurrentSessionManager;

    // 2차 인증(2FA) 임시 세션 캐시 (TTL 180초)
    private final Map<String, TwoFactorSession> twoFactorCache = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class TwoFactorSession {
        private String txSeq;
        private String memberId;
        private String otpCode;
        private MemberDto member;
        private long expireTimeMillis;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto login(LoginRequestDto reqDto, HttpServletRequest request) {
        String memberId = reqDto.getMemberId();
        String password = reqDto.getPassword();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("[로그인 시도] ID={}, 유형={}, IP={}", memberId, reqDto.getMemberType(), clientIp);

        if (!StringUtils.hasText(memberId) || !StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1. 회원 정보 조회
        MemberDto member = memberMapper.selectMemberById(memberId);
        if (member == null) {
            recordLoginLog(memberId, reqDto.getMemberType(), "PASSWORD", "N", "MEMBER_NOT_FOUND", clientIp, userAgent);
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 2. 계정 잠금 상태 확인 (5회 실패 잠금)
        if ("Y".equalsIgnoreCase(member.getLockYn()) || (member.getLoginFailCnt() != null && member.getLoginFailCnt() >= 5)) {
            recordLoginLog(memberId, member.getMemberType(), "PASSWORD", "N", "ACCOUNT_LOCKED", clientIp, userAgent);
            throw new BusinessException(ErrorCode.LOGIN_ACCOUNT_LOCKED);
        }

        // 3. 비밀번호 일치 검증 (SHA-256 해시)
        String inputHash = hashPassword(password);
        if (!inputHash.equals(member.getPassword())) {
            // 실패 횟수 증가
            memberMapper.increaseLoginFailCount(memberId);
            int failCnt = (member.getLoginFailCnt() != null ? member.getLoginFailCnt() : 0) + 1;
            
            if (failCnt >= 5) {
                memberMapper.lockMember(memberId);
                recordLoginLog(memberId, member.getMemberType(), "PASSWORD", "N", "PW_FAIL_LOCKED_5", clientIp, userAgent);
                log.warn("[계정 잠금] ID={} 5회 비밀번호 오류로 계정 잠금 처리", memberId);
                throw new BusinessException(ErrorCode.LOGIN_ACCOUNT_LOCKED);
            }

            recordLoginLog(memberId, member.getMemberType(), "PASSWORD", "N", "PW_MISMATCH_CNT_" + failCnt, clientIp, userAgent);
            throw new BusinessException(ErrorCode.LOGIN_PASSWORD_MISMATCH);
        }

        // 4. 회원 유형별 2차 인증(2FA) 분기 정책
        // 상담사(COUNSELOR) 및 관리자(ADMIN)는 전자금융감독규정에 따라 2차 인증(SMS OTP) 필수!
        if ("COUNSELOR".equals(member.getMemberType()) || "ADMIN".equals(member.getMemberType())) {
            String twoFactorTx = "TX_2FA_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 9000 + 1000);
            String otpCode = String.format("%06d", (int)(Math.random() * 900000 + 100000));

            // 2차 인증 문자 발송 (SMS 모듈 접속)
            String smsMessage = String.format("[저축은행] %s 로그인 2차 인증번호는 [%s]입니다. (3분 유효)",
                    "COUNSELOR".equals(member.getMemberType()) ? "상담사" : "관리자", otpCode);

            smsGatewayClient.sendSms(SmsSendRequestDto.builder()
                    .destPhone(member.getCellNo())
                    .sendPhone("1588-0000")
                    .message(smsMessage)
                    .reqTxSeq(twoFactorTx)
                    .build());

            // 2FA 캐시 저장 (3분 유효)
            twoFactorCache.put(twoFactorTx, TwoFactorSession.builder()
                    .txSeq(twoFactorTx)
                    .memberId(memberId)
                    .otpCode(otpCode)
                    .member(member)
                    .expireTimeMillis(System.currentTimeMillis() + (180 * 1000))
                    .build());

            log.info("[2차 인증 요구] ID={}, 2FA_Tx={}, OTP={}", memberId, twoFactorTx, otpCode);

            return LoginResponseDto.builder()
                    .success(true)
                    .requiresTwoFactor(true)
                    .twoFactorTxSeq(twoFactorTx)
                    .maskedPhone(MaskingUtil.maskPhone(member.getCellNo()))
                    .testOtpCode(otpCode) // 테스트 편의 자동입력 안내
                    .message("등록된 휴대폰번호로 2차 인증번호(OTP)가 전송되었습니다.")
                    .build();
        }

        // 5. 개인 고객(INDIVIDUAL): 1차 검증으로 로그인 최종 완료
        return createLoginSuccessResponse(member, "PASSWORD", request, clientIp, userAgent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto verifyTwoFactor(TwoFactorVerifyDto verifyDto, HttpServletRequest request) {
        String txSeq = verifyDto.getTwoFactorTxSeq();
        String otpCode = verifyDto.getOtpCode();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("[2차 인증 검증] TxSeq={}, 입력OTP={}", txSeq, otpCode);

        TwoFactorSession session = twoFactorCache.get(txSeq);
        if (session == null || System.currentTimeMillis() > session.getExpireTimeMillis()) {
            throw new BusinessException(ErrorCode.AUTH_OTP_NOT_FOUND);
        }

        if (!session.getOtpCode().equals(otpCode)) {
            recordLoginLog(session.getMemberId(), session.getMember().getMemberType(), "TWO_FACTOR_OTP", "N", "2FA_OTP_MISMATCH", clientIp, userAgent);
            throw new BusinessException(ErrorCode.LOGIN_TWO_FACTOR_MISMATCH);
        }

        // 2차 인증 통과 -> 최종 로그인 세션 발급
        twoFactorCache.remove(txSeq);
        return createLoginSuccessResponse(session.getMember(), "TWO_FACTOR_OTP", request, clientIp, userAgent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto loginWithNiceCi(NiceLoginRequestDto niceDto, HttpServletRequest request) {
        String ciVal = niceDto.getCiVal();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("[NICE ID 본인인증 로그인] CI={}, 고객명={}", 
                (ciVal != null && ciVal.length() > 10 ? ciVal.substring(0, 10) + "..." : ciVal), 
                niceDto.getCustNm());

        if (!StringUtils.hasText(ciVal)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // CI로 등록된 회원 조회
        MemberDto member = memberMapper.selectMemberByCi(ciVal);
        if (member == null) {
            // 본인인증 완료 고객 신규 가입 자동 처리
            String newMemberId = "cust_" + System.currentTimeMillis() % 1000000;
            member = MemberDto.builder()
                    .memberId(newMemberId)
                    .memberType("INDIVIDUAL")
                    .roleCd("ROLE_CUSTOMER")
                    .password(hashPassword("Password123!"))
                    .memberNm(StringUtils.hasText(niceDto.getCustNm()) ? niceDto.getCustNm() : "인증고객")
                    .cellNo(StringUtils.hasText(niceDto.getCellNo()) ? niceDto.getCellNo() : "01000000000")
                    .email(newMemberId + "@bank.co.kr")
                    .ciVal(ciVal)
                    .build();
            memberMapper.insertMember(member);
            log.info("[NICE 본인인증 신규회원 가입 완료] ID={}, 이름={}", newMemberId, member.getMemberNm());
        }

        if ("Y".equalsIgnoreCase(member.getLockYn())) {
            // 본인인증을 통과한 경우 계정 잠금 자동 해제 정책 적용
            memberMapper.unlockMember(member.getMemberId());
            member.setLockYn("N");
            member.setLoginFailCnt(0);
            log.info("[NICE 본인인증 계정잠금 해제] ID={}", member.getMemberId());
        }

        return createLoginSuccessResponse(member, "NICE_CI_AUTH", request, clientIp, userAgent);
    }

    @Override
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSessionDto user = (UserSessionDto) session.getAttribute(SESSION_USER_KEY);
            if (user != null) {
                concurrentSessionManager.removeSession(user.getMemberId());
                log.info("[로그아웃 완료] ID={}", user.getMemberId());
            }
            session.invalidate();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockAccount(String memberId) {
        memberMapper.unlockMember(memberId);
        log.info("[계정 잠금 강제 해제] ID={}", memberId);
    }

    /**
     * 로그인 최종 성공 처리:
     * 1) 실패 카운트 초기화 및 최종 로그인일시 갱신
     * 2) 중복 로그인 세션 만료 제어
     * 3) 사용자 세션(UserSessionDto) 등록 및 타임아웃(개인 30분, 상담사/관리자 15분) 부여
     * 4) 전자금융 로그인 감사 로그 영속화
     */
    private LoginResponseDto createLoginSuccessResponse(MemberDto member, String loginMethod,
                                                        HttpServletRequest request, String clientIp, String userAgent) {
        String memberId = member.getMemberId();
        memberMapper.updateLoginSuccess(memberId);

        // 세션 타임아웃 결정: 개인 30분, 상담사/관리자 15분 (보안 강화)
        int timeoutMinutes = "INDIVIDUAL".equals(member.getMemberType()) ? 30 : 15;

        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(timeoutMinutes * 60);

        UserSessionDto sessionUser = UserSessionDto.builder()
                .memberId(memberId)
                .memberNm(member.getMemberNm())
                .memberType(member.getMemberType())
                .roleCd(member.getRoleCd())
                .cellNo(member.getCellNo())
                .email(member.getEmail())
                .ciVal(member.getCiVal())
                .sessionId(session.getId())
                .clientIp(clientIp)
                .loginTime(LocalDateTime.now())
                .timeoutMinutes(timeoutMinutes)
                .build();

        session.setAttribute(SESSION_USER_KEY, sessionUser);

        // 중복 로그인 제어 (기존 세션 만료)
        concurrentSessionManager.registerSession(memberId, session);

        // 감사 로그 기록
        recordLoginLog(memberId, member.getMemberType(), loginMethod, "Y", null, clientIp, userAgent);

        log.info("[로그인 최종 성공] ID={}, 이름={}, 유형={}, 권한={}, 타임아웃={}분",
                memberId, member.getMemberNm(), member.getMemberType(), member.getRoleCd(), timeoutMinutes);

        String redirectUrl = "/board/list";
        if ("ADMIN".equals(member.getMemberType())) {
            redirectUrl = "/common/auth/ci-migration"; // 관리자는 CI 이관/관리자 페이지 우선 연결
        }

        return LoginResponseDto.builder()
                .success(true)
                .requiresTwoFactor(false)
                .message("성공적으로 로그인되었습니다.")
                .sessionUser(sessionUser)
                .redirectUrl(redirectUrl)
                .build();
    }

    private void recordLoginLog(String memberId, String memberType, String loginMethod,
                                String successYn, String failReason, String clientIp, String userAgent) {
        try {
            LoginLogDto logDto = LoginLogDto.builder()
                    .memberId(memberId)
                    .memberType(memberType)
                    .loginMethod(loginMethod)
                    .loginSuccessYn(successYn)
                    .failReason(failReason)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build();
            memberMapper.insertLoginLog(logDto);
        } catch (Exception e) {
            log.error("로그인 감사 로그 기록 실패: {}", e.getMessage());
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 암호화 오류", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
