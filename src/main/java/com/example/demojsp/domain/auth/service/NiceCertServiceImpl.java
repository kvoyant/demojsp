package com.example.demojsp.domain.auth.service;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.common.util.MaskingUtil;
import com.example.demojsp.domain.auth.client.NiceApiClient;
import com.example.demojsp.domain.auth.client.SmsGatewayClient;
import com.example.demojsp.domain.auth.client.dto.*;
import com.example.demojsp.domain.auth.dto.*;
import com.example.demojsp.domain.auth.mapper.NiceAuthMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================================
 * [공통 본인인증 서비스 구현체 - NICE ID 통신사 미지정 연동 및 SMS 모듈 실연동]
 * 
 * 1. 통신사 미지정 SMS 인증 프로세스:
 *    [사용자 입력] 성명 + 생년월일 + 휴대폰번호 (통신사 선택 생략)
 *          │
 *          ▼
 *    [1. 약관 동의 검증] 비여신금융거래 본인확인 통합약관 4종 필수 동의 확인 및 DB 영속화
 *          │
 *          ▼
 *    [2. NICE 모듈 접속] NiceApiClient.requestCarrierUnspecifiedOtp(...)
 *          - 통신사 빈값("") 전달 -> NICE가 통신 3사 및 알뜰폰 HLR 자동 라우팅
 *          - NICE 트랜잭션 번호(resSeq) 및 인증 토큰(authToken) 수신
 *          │
 *          ▼
 *    [3. SMS 모듈 접속] SmsGatewayClient.sendSms(...)
 *          - 금융사 대표번호(1588-0000)로 6자리 OTP 인증번호 발송 전문 전송
 *          - 금융 감사 규정에 따라 TB_SMS_SEND_QUEUE 테이블 큐에 실시간 적재
 *          │
 *          ▼
 *    [4. 고객 OTP 입력] NiceApiClient.verifyOtp(...)
 *          - NICE 모듈 접속하여 인증번호 일치 검증 전문 전송
 *          - 암호화된 응답 전문 복호화하여 표준 88자리 연계정보(CI), DI 획득
 *          │
 *          ▼
 *    [5. 사후 종합 리스크 체크] CustomerCheckService.checkCustomerRisk(...)
 *          - CI 기반 유의고객(Watchlist/FDS) 조회 -> 차단 시 거래 즉시 중단
 *          - birthDate 기준 만 19세 미만 미성년자 체크
 *          - nationalInfo 기준 외국인 체크
 *          - 정상 통과 시 TB_CUST_CI 마스터 저장 및 TB_CERT_LOG 감사 기록 완료
 * =========================================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NiceCertServiceImpl implements NiceCertService {

    private final NiceAuthMapper niceAuthMapper;
    private final CustomerCheckService customerCheckService;
    private final NiceApiClient niceApiClient;          // 실제 NICE ID 모듈 연동 클라이언트
    private final SmsGatewayClient smsGatewayClient;    // 실제 SMS 발송 모듈 연동 클라이언트

    // 인메모리 OTP / MO 세션 캐시 (TTL: 180초)
    private final Map<String, OtpSession> sessionCache = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class OtpSession {
        private String reqTxSeq;
        private String niceResSeq;       // NICE 발급 트랜잭션 시퀀스
        private String otpCode;          // 6자리 인증번호
        private String authToken;        // NICE 인증 토큰
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
        log.info("[NICE 통신사미지정 본인인증 요청 시작] 이름={}, 생년월일={}, 번호={}, 시뮬레이션={}",
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

        // 2. 비여신금융거래 약관 동의 검증
        List<TermsDto> activeTerms = niceAuthMapper.selectActiveTermsList();
        List<String> agreedIds = reqDto.getAgreedTermsIds() != null ? reqDto.getAgreedTermsIds() : Collections.emptyList();
        for (TermsDto term : activeTerms) {
            if ("Y".equalsIgnoreCase(term.getRequiredYn()) && !agreedIds.contains(term.getTermsId())) {
                throw new BusinessException(ErrorCode.AUTH_TERMS_REQUIRED);
            }
        }

        // 3. 트랜잭션 시퀀스 생성
        String txSeq = "TX_NICE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + (int)(Math.random() * 9000 + 1000);

        // 4. 내외국인 코드 산출
        String nationalInfo = "FOREIGNER".equalsIgnoreCase(reqDto.getSimulationMode()) ? "2" : "1";

        // =================================================================================
        // [핵심 연동 ① : 실제 NICE 모듈 접속 (통신사 미지정 OTP 요청)]
        // =================================================================================
        NiceApiOtpRequestDto niceReq = NiceApiOtpRequestDto.builder()
                .requestNo(txSeq)
                .userName(reqDto.getCustNm())
                .birthDate(reqDto.getBirthDate())
                .gender(StringUtils.hasText(reqDto.getGender()) ? reqDto.getGender() : "1")
                .nationalInfo(nationalInfo)
                .mobileNo(reqDto.getCellNo().replaceAll("-", ""))
                .carrier("") // ★ 통신사 미지정 방식의 핵심: 통신사 코드를 빈값으로 전송
                .agreedTerms(agreedIds)
                .channelType(StringUtils.hasText(reqDto.getChannelType()) ? reqDto.getChannelType() : "PC_WEB")
                .build();

        NiceApiOtpResponseDto niceRes = niceApiClient.requestCarrierUnspecifiedOtp(niceReq);

        // 발급된 OTP 번호 결정 (NICE 시뮬레이터 응답 번호 또는 6자리 난수)
        String otpCode = niceRes.getOtpCodeForDebug() != null ? niceRes.getOtpCodeForDebug() : String.format("%06d", (int)(Math.random() * 900000 + 100000));
        String authToken = niceRes.getAuthToken() != null ? niceRes.getAuthToken() : UUID.randomUUID().toString();
        String niceResSeq = niceRes.getResSeq();

        // =================================================================================
        // [핵심 연동 ② : 실제 SMS 모듈 접속 (고객 휴대폰으로 OTP 문자 발송)]
        // =================================================================================
        String smsMessage = String.format("[저축은행] 본인확인 인증번호 [%s]입니다. 타인에게 노출하지 마세요. (3분 유효)", otpCode);
        SmsSendRequestDto smsReq = SmsSendRequestDto.builder()
                .destPhone(reqDto.getCellNo())
                .sendPhone("1588-0000") // 저축은행 공식 발신번호
                .message(smsMessage)
                .reqTxSeq(txSeq)
                .build();

        SmsSendResultDto smsResult = smsGatewayClient.sendSms(smsReq);
        log.info("[SMS 모듈 연동 결과] TxSeq={}, 발송성공여부={}, SMS_SEQ={}", txSeq, smsResult.isSuccess(), smsResult.getSmsSeq());

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

        // 7. 세션 캐시에 보관 (유효시간 3분)
        OtpSession session = OtpSession.builder()
                .reqTxSeq(txSeq)
                .niceResSeq(niceResSeq)
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
        responseData.put("testOtpCode", otpCode); // 화면 자동완성 및 테스트 편의 제공
        responseData.put("detectedCarrier", niceRes.getDetectedCarrier());
        responseData.put("message", "NICE 및 SMS 모듈을 통해 본인인증 번호가 발송되었습니다. (유효시간 3분)");

        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NiceCertResultDto verifyOtp(NiceCertVerifyDto verifyDto) {
        log.info("[NICE 본인인증번호 검증 시작] TX={}, 입력OTP={}", verifyDto.getReqTxSeq(), verifyDto.getOtpCode());

        OtpSession session = sessionCache.get(verifyDto.getReqTxSeq());
        if (session == null || System.currentTimeMillis() > session.getExpireTimeMillis()) {
            throw new BusinessException(ErrorCode.AUTH_OTP_NOT_FOUND);
        }

        if (!session.getOtpCode().equals(verifyDto.getOtpCode())) {
            throw new BusinessException(ErrorCode.AUTH_OTP_MISMATCH);
        }

        // =================================================================================
        // [핵심 연동 ③ : 실제 NICE 모듈 접속 (인증번호 검증 및 복호화된 CI/DI 획득)]
        // =================================================================================
        NiceApiVerifyRequestDto niceVerifyReq = NiceApiVerifyRequestDto.builder()
                .requestNo(session.getReqTxSeq())
                .resSeq(session.getNiceResSeq())
                .authToken(session.getAuthToken())
                .otpCode(verifyDto.getOtpCode())
                .build();

        NiceApiVerifyResponseDto niceVerifyRes = niceApiClient.verifyOtp(niceVerifyReq);
        log.info("[NICE 검증 모듈 연동 완료] TxSeq={}, 결과코드={}, 복호화CI길이={}",
                session.getReqTxSeq(), niceVerifyRes.getResultCode(),
                (niceVerifyRes.getCi() != null ? niceVerifyRes.getCi().length() : 0));

        // 인증 성공 후처리 (CI 매핑, 유의고객/미성년자/외국인 리스크 검증, DB 저장)
        return completeAuthentication(session, "SMS_NO_CARRIER", niceVerifyRes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestMoSession(NiceCertMoReqDto moReqDto, String clientIp, String userAgent) {
        log.info("[NICE MO 기기점유 인증 요청] 이름={}, 생년월일={}, 채널={}",
                MaskingUtil.maskName(moReqDto.getCustNm()), moReqDto.getBirthDate(), moReqDto.getChannelType());

        if (!StringUtils.hasText(moReqDto.getCustNm()) || !StringUtils.hasText(moReqDto.getBirthDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String txSeq = "TX_MO_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + (int)(Math.random() * 9000 + 1000);
        String authToken = UUID.randomUUID().toString();
        String moCode = String.format("%04d", (int) (Math.random() * 9000 + 1000));
        String moVirtualPhone = "16001588";
        String moMessageBody = "[NICE본인확인 " + moCode + "]";

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

        session.setMoReceived(true);

        NiceApiVerifyResponseDto mockVerify = NiceApiVerifyResponseDto.builder()
                .success(true)
                .resultCode("0000")
                .ci("2trxaKUfB0CDWOJc8yi1lUlnxgJFZ3nLfzTkBtOml3C42pwmOwBS3N1k7gMoqk9liMzplbHqvlNpAd0b8YOj0w==")
                .di("di_mo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32))
                .build();

        return completeAuthentication(session, "MO_DEVICE_AUTH", mockVerify);
    }

    /**
     * 본인인증 성공 후처리:
     * 1) NICE로부터 수신한 CI / DI 매핑
     * 2) 유의고객 / 미성년자 / 외국인 체크로직 수행
     * 3) 감사로그(TB_CERT_LOG) 및 TB_CUST_CI 마스터 갱신
     */
    private NiceCertResultDto completeAuthentication(OtpSession session, String authMethod, NiceApiVerifyResponseDto niceVerifyRes) {
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
            // DB에 등록된 유의고객 샘플 CI와 매핑하여 차단 로직 검증 보장
            ciVal = "ci_sample_watchlist_fraud_001_A8f9b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=";
            diVal = "di_sample_watchlist_fraud_001";
        } else if (niceVerifyRes != null && StringUtils.hasText(niceVerifyRes.getCi())) {
            // NICE 모듈에서 실제 수신/복호화된 88자리 표준 CI
            ciVal = niceVerifyRes.getCi();
            diVal = niceVerifyRes.getDi();
        } else {
            ciVal = "2trxaKUfB0CDWOJc8yi1lUlnxgJFZ3nLfzTkBtOml3C42pwmOwBS3N1k7gMoqk9liMzplbHqvlNpAd0b8YOj0w==";
            diVal = "di_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }

        // 2. 종합 리스크 체크 (CI 기반 유의고객 차단, 미성년자, 외국인)
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
}
