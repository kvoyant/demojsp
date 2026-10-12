package com.example.demojsp.domain.auth;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.domain.auth.dto.*;
import com.example.demojsp.domain.auth.service.CiMigrationService;
import com.example.demojsp.domain.auth.service.CustomerCheckService;
import com.example.demojsp.domain.auth.service.NiceCertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NiceCertServiceTest {

    @Autowired
    private NiceCertService niceCertService;

    @Autowired
    private CustomerCheckService customerCheckService;

    @Autowired
    private CiMigrationService ciMigrationService;

    @Test
    @DisplayName("비여신금융거래 본인확인 약관 목록 조회 테스트")
    void testGetActiveTermsList() {
        List<TermsDto> terms = niceCertService.getActiveTermsList();
        assertThat(terms).isNotEmpty();
        assertThat(terms).anyMatch(t -> "TERMS_NON_CREDIT_01".equals(t.getTermsId()));
        assertThat(terms).anyMatch(t -> "TERMS_TELCO_ALL".equals(t.getTermsId()));
    }

    @Test
    @DisplayName("통신사 미지정 방식 본인인증 번호 요청 및 검증 - 정상 고객")
    void testRequestAndVerifyOtp_Normal() {
        // 1. 요청 (통신사 파라미터 불필요)
        NiceCertReqDto req = NiceCertReqDto.builder()
                .custNm("홍길동")
                .birthDate("19900515")
                .gender("1")
                .cellNo("01012345678")
                .channelType("PC_WEB")
                .agreedTermsIds(List.of("TERMS_NON_CREDIT_01", "TERMS_NICE_01", "TERMS_TELCO_ALL", "TERMS_PRIVACY_01"))
                .simulationMode("NORMAL")
                .build();

        Map<String, Object> reqRes = niceCertService.requestOtp(req, "127.0.0.1", "JUnit-Test-Agent");
        assertThat(reqRes.get("reqTxSeq")).isNotNull();
        String txSeq = (String) reqRes.get("reqTxSeq");
        String otpCode = (String) reqRes.get("testOtpCode");

        // 2. 검증
        NiceCertVerifyDto verifyDto = NiceCertVerifyDto.builder()
                .reqTxSeq(txSeq)
                .otpCode(otpCode)
                .build();

        NiceCertResultDto result = niceCertService.verifyOtp(verifyDto);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getCiVal()).isNotNull().hasSize(88); // NICE CI 표준 88바이트
        assertThat(result.getRiskCheck().isPass()).isTrue();
        assertThat(result.getRiskCheck().isWatchlist()).isFalse();
        assertThat(result.getRiskCheck().isMinor()).isFalse();
        assertThat(result.getRiskCheck().isForeigner()).isFalse();
    }

    @Test
    @DisplayName("미성년자 (만 19세 미만) 체크로직 영향도 검증")
    void testMinorCheck() {
        // 생년월일 기준 만 나이 계산
        int age = customerCheckService.calculateExactAge("20120301"); // 만 14세
        assertThat(age).isLessThan(19);

        CustomerRiskCheckDto riskCheck = customerCheckService.checkCustomerRisk(
                "ci_sample_minor_123456789012345678901234567890123456789012345678901234567890123456789012345678=",
                "20120301", "1", "이학생");

        assertThat(riskCheck.isMinor()).isTrue();
        assertThat(riskCheck.getMinorGuideMessage()).contains("만 19세 미만 미성년자");
    }

    @Test
    @DisplayName("외국인 고객 체크로직 영향도 검증")
    void testForeignerCheck() {
        CustomerRiskCheckDto riskCheck = customerCheckService.checkCustomerRisk(
                "ci_sample_foreigner_12345678901234567890123456789012345678901234567890123456789012345678901234=",
                "19881105", "2", "존스미스");

        assertThat(riskCheck.isForeigner()).isTrue();
        assertThat(riskCheck.getForeignerGuideMessage()).contains("외국인등록증");
    }

    @Test
    @DisplayName("유의고객 (Watchlist/FDS) CI 기반 차단 로직 검증")
    void testWatchlistBlockedCheck() {
        // DB에 등록된 유의고객 샘플 CI
        String watchlistCi = "ci_sample_watchlist_fraud_001_A8f9b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=";

        CustomerRiskCheckDto riskCheck = customerCheckService.checkCustomerRisk(
                watchlistCi, "19820710", "1", "사고유의자");

        assertThat(riskCheck.isWatchlist()).isTrue();
        assertThat(riskCheck.isBlocked()).isTrue();
        assertThat(riskCheck.isPass()).isFalse();

        // 인증 프로세스 상에서도 최종 BusinessException(AUTH_WATCHLIST_BLOCKED) 발생하는지 검증
        NiceCertReqDto req = NiceCertReqDto.builder()
                .custNm("사고유의자")
                .birthDate("19820710")
                .cellNo("01099991111")
                .agreedTermsIds(List.of("TERMS_NON_CREDIT_01", "TERMS_NICE_01", "TERMS_TELCO_ALL", "TERMS_PRIVACY_01"))
                .simulationMode("WATCHLIST_FRAUD")
                .build();

        Map<String, Object> reqRes = niceCertService.requestOtp(req, "127.0.0.1", "Agent");
        String txSeq = (String) reqRes.get("reqTxSeq");
        String otpCode = (String) reqRes.get("testOtpCode");

        NiceCertVerifyDto verifyDto = NiceCertVerifyDto.builder()
                .reqTxSeq(txSeq)
                .otpCode(otpCode)
                .build();

        assertThatThrownBy(() -> niceCertService.verifyOtp(verifyDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_WATCHLIST_BLOCKED);
    }

    @Test
    @DisplayName("9/30 이기종 CI 데이터 정합성 검증 및 마이그레이션 실행 테스트")
    void testCiMigrationPipeline() {
        // 1. 임시 테이블 샘플 리셋
        ciMigrationService.resetSampleData();

        // 2. 사전 정합성 검증
        int validCount = ciMigrationService.validateStagingRecords();
        assertThat(validCount).isGreaterThan(0);

        // 3. 이관 실행
        CiMigrationDto.ExecutionResult result = ciMigrationService.executeMigration("TEST_ADMIN");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSuccessCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.getFailCount()).isGreaterThanOrEqualTo(1); // 길이 오류 1건
        assertThat(result.getDuplicateCount()).isGreaterThanOrEqualTo(1); // 중복 1건

        // 4. 이력 확인
        List<CiMigrationDto.HistoryRecord> histories = ciMigrationService.getMigrationHistories();
        assertThat(histories).isNotEmpty();
    }
}
