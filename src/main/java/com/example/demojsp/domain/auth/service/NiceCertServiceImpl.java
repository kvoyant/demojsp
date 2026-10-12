package com.example.demojsp.domain.auth.service;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.common.util.MaskingUtil;
import com.example.demojsp.domain.auth.dto.*;
import com.example.demojsp.domain.auth.mapper.NiceAuthMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NiceCertServiceImpl implements NiceCertService {

    private final NiceAuthMapper niceAuthMapper;
    private final CustomerCheckService customerCheckService;

    // 인메모리 OTP / MO 세션 캐시 (TTL: 180초)
    private final Map<String, OtpSession> sessionCache = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class OtpSession {
        private String reqTxSeq;
        private String otpCode;
        private String authToken;
        private String custNm;
        private String birthDate;
        private String gender;
        private String nationalInfo;
        private String cellNo;
        private String authMethod;
        private String simulationMode;
        private long expireTimeMillis;
        private boolean moReceived;
    }

    @Override
    public List<TermsDto> getActiveTermsList() {
        return niceAuthMapper.selectActiveTermsList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestOtp(NiceCertReqDto reqDto, String clientIp, String userAgent) {
        log.info("[NICE 통신사미지정 인증 요청] 이름={}, 생년월일={}, 번호={}, 시뮬레이션={}",
                MaskingUtil.maskName(reqDto.getCustNm()),
                reqDto.getBirthDate(),
                MaskingUtil.maskPhone(reqDto.getCellNo()),
                reqDto.getSimulationMode());

        // 1. 필수 입력값 검증 (통신사 선택 필드 없음!)
        if (!StringUtils.hasText(reqDto.getCustNm()) ||
            !StringUtils.hasText(reqDto.getBirthDate()) ||
            !StringUtils.hasText(reqDto.getCellNo())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. 약관 동의 검증
        List<TermsDto> activeTerms = niceAuthMapper.selectActiveTermsList();
        List<String> agreedIds = reqDto.getAgreedTermsIds() != null ? reqDto.getAgreedTermsIds() : Collections.emptyList();
        for (TermsDto term : activeTerms) {
            if ("Y".equalsIgnoreCase(term.getRequiredYn()) && !agreedIds.contains(term.getTermsId())) {
                throw new BusinessException(ErrorCode.AUTH_TERMS_REQUIRED);
            }
        }

        // 3. 트랜잭션 시퀀스 및 토큰 생성
        String txSeq = "TX_NICE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + (int)(Math.random() * 9000 + 1000);
        String authToken = UUID.randomUUID().toString();

        // 4. 통신사 미지정 방식 일회용 OTP 번호 생성 (6자리)
        String otpCode = String.format("%06d", (int) (Math.random() * 900000 + 100000));

        // 5. 약관 동의 이력 영속화
        for (String termsId : agreedIds) {
            niceAuthMapper.insertTermsAgreeLog(txSeq, termsId, "Y");
        }

        // 6. 감사 로그 기록 (REQUESTED)
        CertLogDto certLog = CertLogDto.builder()
                .reqTxSeq(txSeq)
                .channelType(StringUtils.hasText(reqDto.getChannelType()) ? reqDto.getChannelType() : "PC_WEB")
                .authMethod("SMS_NO_CARRIER")
                .custNm(reqDto.getCustNm())
                .birthDate(reqDto.getBirthDate())
                .cellNo(reqDto.getCellNo().replaceAll("-", ""))
                .authToken(authToken)
                .authStatus("REQUESTED")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        niceAuthMapper.insertCertLog(certLog);

        // 7. 내외국인 기본값 (시뮬레이션 모드 반영)
        String nationalInfo = "1"; // 1: 내국인
        if ("FOREIGNER".equalsIgnoreCase(reqDto.getSimulationMode())) {
            nationalInfo = "2";    // 2: 외국인
        }

        // 8. 세션 캐시에 보관 (유효시간 3분)
        OtpSession session = OtpSession.builder()
                .reqTxSeq(txSeq)
                .otpCode(otpCode)
                .authToken(authToken)
                .custNm(reqDto.getCustNm())
                .birthDate(reqDto.getBirthDate())
                .gender(StringUtils.hasText(reqDto.getGender()) ? reqDto.getGender() : "1")
                .nationalInfo(nationalInfo)
                .cellNo(reqDto.getCellNo().replaceAll("-", ""))
                .authMethod("SMS_NO_CARRIER")
                .simulationMode(reqDto.getSimulationMode())
                .expireTimeMillis(System.currentTimeMillis() + (180 * 1000))
                .moReceived(false)
                .build();
        sessionCache.put(txSeq, session);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("reqTxSeq", txSeq);
        responseData.put("authToken", authToken);
        responseData.put("expiresInSeconds", 180);
        responseData.put("testOtpCode", otpCode); // 개발/테스트 편의용 화면 자동완성 안내
        responseData.put("message", "통신사 미지정 본인인증 번호가 전송되었습니다. (유효시간 3분)");

        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NiceCertResultDto verifyOtp(NiceCertVerifyDto verifyDto) {
        log.info("[NICE 본인인증번호 검증] TX={}, 입력OTP={}", verifyDto.getReqTxSeq(), verifyDto.getOtpCode());

        OtpSession session = sessionCache.get(verifyDto.getReqTxSeq());
        if (session == null || System.currentTimeMillis() > session.getExpireTimeMillis()) {
            throw new BusinessException(ErrorCode.AUTH_OTP_NOT_FOUND);
        }

        if (!session.getOtpCode().equals(verifyDto.getOtpCode())) {
            throw new BusinessException(ErrorCode.AUTH_OTP_MISMATCH);
        }

        // 인증 성공 -> CI / DI 발급 및 종합 비즈니스 체크 진행
        return completeAuthentication(session, "SMS_NO_CARRIER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestMoSession(NiceCertMoReqDto moReqDto, String clientIp, String userAgent) {
        log.info("[NICE MO 기기점유 인증 요청] 이름={}, 생년월일={}, 채널={}",
                MaskingUtil.maskName(moReqDto.getCustNm()), moReqDto.getBirthDate(), moReqDto.getChannelType());

        // 1. 필수 입력값 검증
        if (!StringUtils.hasText(moReqDto.getCustNm()) || !StringUtils.hasText(moReqDto.getBirthDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. 트랜잭션 시퀀스 생성
        String txSeq = "TX_MO_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + (int)(Math.random() * 9000 + 1000);
        String authToken = UUID.randomUUID().toString();

        // 3. MO 단말 발송용 인증코드 (4자리 숫자) 및 NICE 수신번호
        String moCode = String.format("%04d", (int) (Math.random() * 9000 + 1000));
        String moVirtualPhone = "16001588";
        String moMessageBody = "[NICE본인확인 " + moCode + "]";

        // 4. 감사 로그 기록
        CertLogDto certLog = CertLogDto.builder()
                .reqTxSeq(txSeq)
                .channelType(StringUtils.hasText(moReqDto.getChannelType()) ? moReqDto.getChannelType() : "APP")
                .authMethod("MO_DEVICE_AUTH")
                .custNm(moReqDto.getCustNm())
                .birthDate(moReqDto.getBirthDate())
                .cellNo("MO_DETECTED")
                .authToken(authToken)
                .authStatus("REQUESTED")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        niceAuthMapper.insertCertLog(certLog);

        String nationalInfo = "FOREIGNER".equalsIgnoreCase(moReqDto.getSimulationMode()) ? "2" : "1";

        OtpSession session = OtpSession.builder()
                .reqTxSeq(txSeq)
                .otpCode(moCode)
                .authToken(authToken)
                .custNm(moReqDto.getCustNm())
                .birthDate(moReqDto.getBirthDate())
                .gender("1")
                .nationalInfo(nationalInfo)
                .cellNo("01099998888")
                .authMethod("MO_DEVICE_AUTH")
                .simulationMode(moReqDto.getSimulationMode())
                .expireTimeMillis(System.currentTimeMillis() + (300 * 1000))
                .moReceived(false)
                .build();
        sessionCache.put(txSeq, session);

        Map<String, Object> result = new HashMap<>();
        result.put("reqTxSeq", txSeq);
        result.put("moVirtualPhone", moVirtualPhone);
        result.put("moCode", moCode);
        result.put("moMessageBody", moMessageBody);
        result.put("smsUri", "sms:" + moVirtualPhone + "?body=" + moMessageBody);
        result.put("expiresInSeconds", 300);
        result.put("message", "MO 기기 점유 인증 요청이 생성되었습니다. 안내된 번호로 문자를 발송해 주세요.");

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NiceCertResultDto checkMoStatus(String reqTxSeq) {
        OtpSession session = sessionCache.get(reqTxSeq);
        if (session == null || System.currentTimeMillis() > session.getExpireTimeMillis()) {
            throw new BusinessException(ErrorCode.AUTH_OTP_NOT_FOUND);
        }

        // 데모 환경에서는 사용자가 MO 확인 버튼을 누르면 단말 문자 수신 성공으로 시뮬레이션 처리
        session.setMoReceived(true);

        return completeAuthentication(session, "MO_DEVICE_AUTH");
    }

    /**
     * 본인인증 성공 후처리:
     * 1) CI / DI 생성 또는 매핑
     * 2) 유의고객 / 미성년자 / 외국인 체크로직 수행
     * 3) 감사로그 및 TB_CUST_CI 마스터 갱신
     */
    private NiceCertResultDto completeAuthentication(OtpSession session, String authMethod) {
        String txSeq = session.getReqTxSeq();
        String custNm = session.getCustNm();
        String birthDate = session.getBirthDate();
        String cellNo = session.getCellNo();
        String nationalInfo = session.getNationalInfo();
        String simulationMode = session.getSimulationMode();

        // 1. CI / DI 산출
        String ciVal;
        String diVal;

        if ("WATCHLIST_FRAUD".equalsIgnoreCase(simulationMode) || custNm.contains("사고유의")) {
            // DB에 등록된 유의고객 샘플 CI와 매핑
            ciVal = "ci_sample_watchlist_fraud_001_A8f9b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=";
            diVal = "di_sample_watchlist_fraud_001";
        } else {
            // 정상 고객용 88자리 표준 규격 CI 생성 (해시 기반 고유값)
            ciVal = generateStandardCi(custNm, birthDate, cellNo);
            diVal = "di_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }

        // 2. 종합 리스크 체크 (유의고객, 미성년자, 외국인)
        CustomerRiskCheckDto riskCheck = customerCheckService.checkCustomerRisk(ciVal, birthDate, nationalInfo, custNm);

        // 3. 감사 이력(TB_CERT_LOG) 갱신
        CertLogDto certLog = CertLogDto.builder()
                .reqTxSeq(txSeq)
                .ciVal(ciVal)
                .build();

        if (riskCheck.isBlocked()) {
            certLog.setAuthStatus("RISK_BLOCKED");
            certLog.setErrCode(ErrorCode.AUTH_WATCHLIST_BLOCKED.getCode());
            certLog.setErrMsg(riskCheck.getRiskReason());
            niceAuthMapper.updateCertLog(certLog);
            sessionCache.remove(txSeq);

            log.warn("[인증 차단] 유의고객 명의자: {} (사유: {})", custNm, riskCheck.getRiskReason());
            throw new BusinessException(ErrorCode.AUTH_WATCHLIST_BLOCKED);
        } else {
            certLog.setAuthStatus("VERIFIED");
            niceAuthMapper.updateCertLog(certLog);

            // 4. 고객 CI 마스터 저장 (정상 통과 시)
            CustCiDto custCi = CustCiDto.builder()
                    .ciVal(ciVal)
                    .diVal(diVal)
                    .custId("CUST_" + System.currentTimeMillis() % 1000000)
                    .custNm(custNm)
                    .birthDate(birthDate)
                    .gender(session.getGender())
                    .nationalInfo(nationalInfo)
                    .cellNo(cellNo)
                    .build();
            niceAuthMapper.insertOrUpdateCustCi(custCi);
        }

        sessionCache.remove(txSeq);

        return NiceCertResultDto.builder()
                .reqTxSeq(txSeq)
                .authenticated(true)
                .authMethod(authMethod)
                .ciVal(ciVal)
                .ciMasked(MaskingUtil.maskCi(ciVal))
                .diVal(diVal)
                .custNm(custNm)
                .birthDate(birthDate)
                .gender(session.getGender())
                .nationalInfo(nationalInfo)
                .cellNo(cellNo)
                .cellNoMasked(MaskingUtil.maskPhone(cellNo))
                .riskCheck(riskCheck)
                .authCompleteTime(LocalDateTime.now())
                .build();
    }

    /**
     * NICE 88바이트 연계정보(CI) 표준 포맷 시뮬레이터
     * 고유 식별자 해시 및 Base64 인코딩으로 정확히 88글자 생성
     */
    private String generateStandardCi(String name, String birthDate, String phone) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            String raw = name + "_" + birthDate + "_" + phone + "_NICE_FINANCIAL_SALT_2026";
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(hash); // 88글자 (SHA-512 = 64바이트 -> Base64 88글자)
            return base64;
        } catch (Exception e) {
            return "ci_standard_88char_sample_" + UUID.randomUUID().toString().replace("-", "") + "0123456789abcde=";
        }
    }
}
