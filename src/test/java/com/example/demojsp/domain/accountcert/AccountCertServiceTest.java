package com.example.demojsp.domain.accountcert;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.domain.accountcert.dto.*;
import com.example.demojsp.domain.accountcert.mapper.AccountCertMapper;
import com.example.demojsp.domain.accountcert.service.AccountCertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountCertServiceTest {

    @Autowired
    private AccountCertService accountCertService;

    @Autowired
    private AccountCertMapper accountCertMapper;

    @Test
    @DisplayName("1. 금융기관(은행/증권) 전체 코드 목록 조회")
    void testGetBankCodeList() {
        List<BankCodeDto> bankList = accountCertService.getBankCodeList();
        assertThat(bankList).isNotEmpty();
        assertThat(bankList).anyMatch(b -> "004".equals(b.getBankCd()) && "KB국민은행".equals(b.getBankNm()));
        assertThat(bankList).anyMatch(b -> "090".equals(b.getBankCd()) && "카카오뱅크".equals(b.getBankNm()));
    }

    @Test
    @DisplayName("2. [요구사항 1] 대출신청 대출금입금/출금계좌 정보 불러와 자동완성 처리 검증")
    void testGetLoanAccount_AutoFill() {
        // 대출번호 LN202610130001 (홍길동, KB국민은행 123402-04-567890)
        LoanAccountDto loanAccount = accountCertService.getLoanAccount("LN202610130001", null);

        assertThat(loanAccount).isNotNull();
        assertThat(loanAccount.getLoanNo()).isEqualTo("LN202610130001");
        assertThat(loanAccount.getBankCd()).isEqualTo("004");
        assertThat(loanAccount.getBankNm()).isEqualTo("KB국민은행");
        assertThat(loanAccount.getAccountHolderNm()).isEqualTo("홍길동");
        assertThat(loanAccount.getAccountNo()).contains("123402");
    }

    @Test
    @DisplayName("3. [요구사항 1] 대출계좌 자동완성 후 1원 송금 요청 성공 (적요 생성, 10분 유효시간)")
    void testRequestOneWonTransfer_WithLoanAccount() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("LOAN_APPLY")
                .loanNo("LN202610130001")
                .custNm("홍길동")
                .bankCd("004")
                .accountNo("12340204567890")
                .isModified("N")
                .build();

        OneWonTransferResDto res = accountCertService.requestOneWonTransfer(req, request);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getReqTxSeq()).startsWith("TX_ACCT_");
        assertThat(res.getPrintText()).startsWith("저축");
        assertThat(res.getTestAuthCode()).hasSize(4);
        assertThat(res.getExpiresInSeconds()).isEqualTo(600); // 10분
        assertThat(res.getAccountHolderNm()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("4. [요구사항 2] 수정기능 제공 - 대출계좌 대신 다른 은행/계좌 선택 및 입력 후 1원 송금 성공")
    void testRequestOneWonTransfer_ModifiedAccount() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 고객이 대출계좌 대신 카카오뱅크 타 계좌로 변경 입력한 케이스
        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("LOAN_APPLY")
                .loanNo("LN202610130001")
                .custNm("홍길동")
                .bankCd("090") // 카카오뱅크
                .accountNo("3333019876543")
                .isModified("Y") // 수정 여부 플래그
                .build();

        OneWonTransferResDto res = accountCertService.requestOneWonTransfer(req, request);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getBankNm()).isEqualTo("카카오뱅크");

        // DB 이력에 수정 여부가 Y로 기록되었는지 확인
        AccountCertHistDto hist = accountCertMapper.selectAccountCertHistByTxSeq(res.getReqTxSeq());
        assertThat(hist.getIsModified()).isEqualTo("Y");
    }

    @Test
    @DisplayName("5. 예금주명 불일치 시 ACCOUNT_HOLDER_MISMATCH 오류 차단")
    void testAccountHolderMismatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 신청인 실명은 '김철수'인데 계좌 예금주는 '홍길동'인 경우
        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("COMMON")
                .custNm("김철수") // 불일치 실명
                .bankCd("004")
                .accountNo("12340204567890")
                .build();

        assertThatThrownBy(() -> accountCertService.requestOneWonTransfer(req, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_HOLDER_MISMATCH);
    }

    @Test
    @DisplayName("6. 거래정지/지급제한 계좌 시 ACCOUNT_STATUS_INVALID 오류 차단")
    void testSuspendedAccountBlocked() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 거래정지 시뮬레이션 계좌번호 (끝자리 8888)
        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("COMMON")
                .custNm("홍길동")
                .bankCd("004")
                .accountNo("12345678888")
                .build();

        assertThatThrownBy(() -> accountCertService.requestOneWonTransfer(req, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_STATUS_INVALID);
    }

    @Test
    @DisplayName("7. 1원 인증코드 일치 시 최종 검증 성공 및 토큰 발급")
    void testVerifyOneWon_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1) 1원 송금
        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("LOAN_APPLY")
                .loanNo("LN202610130001")
                .custNm("홍길동")
                .bankCd("004")
                .accountNo("12340204567890")
                .build();
        OneWonTransferResDto transferRes = accountCertService.requestOneWonTransfer(req, request);

        // 2) 정상 4자리 인증코드 검증
        OneWonVerifyReqDto verifyReq = OneWonVerifyReqDto.builder()
                .reqTxSeq(transferRes.getReqTxSeq())
                .authCode(transferRes.getTestAuthCode())
                .build();

        OneWonVerifyResDto verifyRes = accountCertService.verifyOneWon(verifyReq, request);

        assertThat(verifyRes.isSuccess()).isTrue();
        assertThat(verifyRes.getAuthConfirmToken()).isNotBlank();
        assertThat(verifyRes.getAccountHolderNm()).isEqualTo("홍길동");

        // DB 이력 상태가 VERIFIED인지 검증
        AccountCertHistDto hist = accountCertMapper.selectAccountCertHistByTxSeq(transferRes.getReqTxSeq());
        assertThat(hist.getStatus()).isEqualTo("VERIFIED");
        assertThat(hist.getVerifyDt()).isNotNull();
    }

    @Test
    @DisplayName("8. 1원 인증코드 5회 연속 오류 시 계정 잠금 및 인증 취소 (ACCOUNT_AUTH_FAIL_LIMIT)")
    void testVerifyOneWon_FailLimit5() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1) 1원 송금
        OneWonTransferReqDto req = OneWonTransferReqDto.builder()
                .purpose("COMMON")
                .custNm("홍길동")
                .bankCd("004")
                .accountNo("12340204567890")
                .build();
        OneWonTransferResDto transferRes = accountCertService.requestOneWonTransfer(req, request);

        OneWonVerifyReqDto wrongVerify = OneWonVerifyReqDto.builder()
                .reqTxSeq(transferRes.getReqTxSeq())
                .authCode("0000") // 틀린 코드
                .build();

        // 1~4회 틀림 -> ACCOUNT_AUTH_MISMATCH
        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> accountCertService.verifyOneWon(wrongVerify, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACCOUNT_AUTH_MISMATCH);
        }

        // 5회 틀림 -> ACCOUNT_AUTH_FAIL_LIMIT (잠금)
        assertThatThrownBy(() -> accountCertService.verifyOneWon(wrongVerify, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_AUTH_FAIL_LIMIT);

        // DB 이력 상태가 FAILED인지 검증
        AccountCertHistDto hist = accountCertMapper.selectAccountCertHistByTxSeq(transferRes.getReqTxSeq());
        assertThat(hist.getStatus()).isEqualTo("FAILED");
        assertThat(hist.getFailCnt()).isEqualTo(5);
    }
}
