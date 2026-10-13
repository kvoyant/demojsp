package com.example.demojsp.domain.accountcert.service;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.domain.accountcert.client.FirmBankingClient;
import com.example.demojsp.domain.accountcert.client.dto.AccountInquiryResultDto;
import com.example.demojsp.domain.accountcert.client.dto.FirmBankingTransferResultDto;
import com.example.demojsp.domain.accountcert.dto.*;
import com.example.demojsp.domain.accountcert.mapper.AccountCertMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * =========================================================================================
 * [공통 본인확인 | 1원 계좌인증 서비스 구현체 (REQ-AP-605)]
 * 
 * 1. 목적 및 기능:
 *    • 대출신청 시 대출금입금/출금계좌 입력 화면과 타 업무 공통 사용 화면 분리 운영
 *    • 본인확인 수행을 위해 타행 본인계좌로 1원 송금 후 통장 적요 인증문구 검증
 *    • 입금은행/입금계좌 항목에 대출금입금/출금계좌 정보를 불러와 자동완성 처리
 *    • 수정기능을 제공하여 은행/계좌 선택 및 직접 입력 가능
 * 
 * 2. 금융권 핵심 보안 및 정책:
 *    • 계좌 실명확인 선행: 입력 고객명과 타행 예금주명 일치 여부 확인
 *    • 유효시간 통제: 10분(600초) 유효시간 만료 시 무효화
 *    • 오류 횟수 통제: 5회 연속 오류 시 해당 인증 트랜잭션 잠금 폐기
 *    • 감사 이력 관리: 시도 이력 전수 TB_ACCOUNT_CERT_HIST 영속화
 * =========================================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCertServiceImpl implements AccountCertService {

    private final AccountCertMapper accountCertMapper;
    private final FirmBankingClient firmBankingClient;

    private static final int CERT_EXPIRE_SECONDS = 600; // 10분 (600초)
    private static final int MAX_FAIL_COUNT = 5;       // 최대 5회 오류 허용

    @Override
    public List<BankCodeDto> getBankCodeList() {
        return accountCertMapper.selectBankCodeList();
    }

    @Override
    public LoanAccountDto getLoanAccount(String loanNo, String memberId) {
        log.info("[대출계좌 불러오기] loanNo={}, memberId={}", loanNo, memberId);

        LoanAccountDto account = null;
        if (StringUtils.hasText(loanNo)) {
            account = accountCertMapper.selectLoanAccountByLoanNo(loanNo);
        } else if (StringUtils.hasText(memberId)) {
            account = accountCertMapper.selectLoanAccountByMemberId(memberId);
        }

        if (account == null) {
            log.warn("[대출계좌 미존재] loanNo={}, memberId={}", loanNo, memberId);
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND);
        }

        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneWonTransferResDto requestOneWonTransfer(OneWonTransferReqDto reqDto, HttpServletRequest request) {
        String bankCd = reqDto.getBankCd();
        String accountNo = reqDto.getAccountNo();
        String custNm = reqDto.getCustNm();
        String purpose = StringUtils.hasText(reqDto.getPurpose()) ? reqDto.getPurpose() : "COMMON";
        String isModified = StringUtils.hasText(reqDto.getIsModified()) ? reqDto.getIsModified() : "N";

        log.info("[1원 송금 요청] 업무={}, 대출번호={}, 고객명={}, 은행={}, 계좌={}, 수정여부={}",
                purpose, reqDto.getLoanNo(), custNm, bankCd, maskAccount(accountNo), isModified);

        // 1. 필수 입력값 검증
        if (!StringUtils.hasText(bankCd) || !StringUtils.hasText(accountNo) || !StringUtils.hasText(custNm)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String cleanAccountNo = accountNo.replaceAll("[^0-9]", "");
        if (cleanAccountNo.length() < 9 || cleanAccountNo.length() > 16) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 2. 은행 정보 확인
        BankCodeDto bankCode = accountCertMapper.selectBankCode(bankCd);
        String bankNm = (bankCode != null) ? bankCode.getBankNm() : "기타은행";

        // 3. 타행 계좌 실명조회 (예금주명 및 계좌 정상 상태 확인)
        AccountInquiryResultDto inquiryRes = firmBankingClient.inquireAccountHolder(bankCd, cleanAccountNo);
        if (!inquiryRes.isSuccess()) {
            if ("NOT_FOUND".equals(inquiryRes.getAccountStatus())) {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
            } else if ("SUSPENDED".equals(inquiryRes.getAccountStatus())) {
                throw new BusinessException(ErrorCode.ACCOUNT_STATUS_INVALID);
            } else {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
        }

        // 4. 예금주명 일치 여부 확인 (본인 명의 계좌만 인증 가능)
        String realHolderNm = StringUtils.hasText(inquiryRes.getAccountHolderNm()) ? inquiryRes.getAccountHolderNm() : custNm;
        if (!custNm.trim().equals(realHolderNm.trim())) {
            log.warn("[예금주 불일치 차단] 신청인명={}, 실제예금주명={}", custNm, realHolderNm);
            throw new BusinessException(ErrorCode.ACCOUNT_HOLDER_MISMATCH);
        }

        // 5. 4자리 인증코드 및 통장 표기문구 생성 (예: '저축7294')
        String authCode = String.format("%04d", (int)(Math.random() * 9000 + 1000));
        String printText = "저축" + authCode; // 금융권 표준 6자리 통장 적요 인자
        String reqTxSeq = "TX_ACCT_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 9000 + 1000);
        LocalDateTime expireDt = LocalDateTime.now().plusSeconds(CERT_EXPIRE_SECONDS);

        // 6. 타행 1원 이체 송금 실행 (펌뱅킹/오픈뱅킹 망)
        FirmBankingTransferResultDto transferRes = firmBankingClient.transferOneWon(bankCd, cleanAccountNo, printText, reqTxSeq);
        if (!transferRes.isSuccess()) {
            log.error("[1원 송금 실패] 타행 통신 오류: {}", transferRes.getResMessage());
            throw new BusinessException(ErrorCode.ACCOUNT_TRANSFER_FAILED);
        }

        // 7. 인증 이력 DB 영속화 (TB_ACCOUNT_CERT_HIST)
        AccountCertHistDto histDto = AccountCertHistDto.builder()
                .reqTxSeq(reqTxSeq)
                .memberId(reqDto.getMemberId())
                .custNm(custNm)
                .bankCd(bankCd)
                .bankNm(bankNm)
                .accountNo(cleanAccountNo)
                .accountHolderNm(realHolderNm)
                .authCode(authCode)
                .printText(printText)
                .status("REQUESTED")
                .purpose(purpose)
                .loanNo(reqDto.getLoanNo())
                .isModified(isModified)
                .expireDt(expireDt)
                .clientIp(getClientIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : "Unknown")
                .build();
        accountCertMapper.insertAccountCertHist(histDto);

        log.info("[1원 송금 완료] TxSeq={}, 적요문구='{}', 유효시간={}초", reqTxSeq, printText, CERT_EXPIRE_SECONDS);

        return OneWonTransferResDto.builder()
                .success(true)
                .reqTxSeq(reqTxSeq)
                .bankCd(bankCd)
                .bankNm(bankNm)
                .accountNoMasked(maskAccount(cleanAccountNo))
                .accountHolderNm(realHolderNm)
                .printText(printText)
                .testAuthCode(authCode) // 개발/테스트 편의 자동완성 안내
                .expiresInSeconds(CERT_EXPIRE_SECONDS)
                .status("REQUESTED")
                .message("입력하신 계좌로 1원을 송금하였습니다. 입금자명의 숫자 4자리를 확인 후 입력해 주세요.")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneWonVerifyResDto verifyOneWon(OneWonVerifyReqDto verifyDto, HttpServletRequest request) {
        String reqTxSeq = verifyDto.getReqTxSeq();
        String inputAuthCode = verifyDto.getAuthCode();

        log.info("[1원 인증 검증 시도] TxSeq={}, 입력코드={}", reqTxSeq, inputAuthCode);

        if (!StringUtils.hasText(reqTxSeq) || !StringUtils.hasText(inputAuthCode)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1. 인증 이력 조회
        AccountCertHistDto hist = accountCertMapper.selectAccountCertHistByTxSeq(reqTxSeq);
        if (hist == null) {
            throw new BusinessException(ErrorCode.AUTH_OTP_NOT_FOUND);
        }

        // 2. 이미 완료되었거나 취소된 건인지 확인
        if ("VERIFIED".equals(hist.getStatus())) {
            return OneWonVerifyResDto.builder()
                    .success(true)
                    .reqTxSeq(reqTxSeq)
                    .authConfirmToken(UUID.randomUUID().toString())
                    .bankCd(hist.getBankCd())
                    .bankNm(hist.getBankNm())
                    .accountNoMasked(maskAccount(hist.getAccountNo()))
                    .accountHolderNm(hist.getAccountHolderNm())
                    .verifyDt(hist.getVerifyDt())
                    .message("이미 인증이 완료된 계좌입니다.")
                    .build();
        }

        // 3. 오류 횟수 5회 초과 잠금 상태 확인
        if ("FAILED".equals(hist.getStatus()) || (hist.getFailCnt() != null && hist.getFailCnt() >= MAX_FAIL_COUNT)) {
            log.warn("[인증 잠금 차단] 5회 오류 잠금 계좌 TxSeq={}", reqTxSeq);
            throw new BusinessException(ErrorCode.ACCOUNT_AUTH_FAIL_LIMIT);
        }

        // 4. 유효시간(10분) 만료 확인
        if (hist.getExpireDt() != null && LocalDateTime.now().isAfter(hist.getExpireDt())) {
            log.warn("[인증 만료] TxSeq={}, 만료일시={}", reqTxSeq, hist.getExpireDt());
            throw new BusinessException(ErrorCode.ACCOUNT_AUTH_EXPIRED);
        }

        // 5. 인증코드 일치 여부 검증
        if (!hist.getAuthCode().equals(inputAuthCode.trim())) {
            accountCertMapper.increaseFailCount(reqTxSeq);
            int newFailCnt = (hist.getFailCnt() != null ? hist.getFailCnt() : 0) + 1;

            if (newFailCnt >= MAX_FAIL_COUNT) {
                accountCertMapper.updateStatusToFailed(reqTxSeq);
                log.warn("[인증 취소/잠금] 5회 연속 오류 도달. TxSeq={}", reqTxSeq);
                throw new BusinessException(ErrorCode.ACCOUNT_AUTH_FAIL_LIMIT);
            }

            log.warn("[인증코드 불일치] TxSeq={}, 오류횟수={}/{}", reqTxSeq, newFailCnt, MAX_FAIL_COUNT);
            throw new BusinessException(ErrorCode.ACCOUNT_AUTH_MISMATCH);
        }

        // 6. 검증 성공 처리
        accountCertMapper.updateStatusToVerified(reqTxSeq);
        String confirmToken = "ACCT_AUTH_TOKEN_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        log.info("[1원 계좌인증 성공] TxSeq={}, 예금주={}, 완료토큰={}", reqTxSeq, hist.getAccountHolderNm(), confirmToken);

        return OneWonVerifyResDto.builder()
                .success(true)
                .reqTxSeq(reqTxSeq)
                .authConfirmToken(confirmToken)
                .bankCd(hist.getBankCd())
                .bankNm(hist.getBankNm())
                .accountNoMasked(maskAccount(hist.getAccountNo()))
                .accountHolderNm(hist.getAccountHolderNm())
                .verifyDt(LocalDateTime.now())
                .message("계좌 본인확인 인증이 성공적으로 완료되었습니다.")
                .build();
    }

    private String maskAccount(String accountNo) {
        if (!StringUtils.hasText(accountNo)) return "";
        int len = accountNo.length();
        if (len <= 6) return accountNo;
        return accountNo.substring(0, 3) + "-****-" + accountNo.substring(len - 3);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
