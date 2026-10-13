package com.example.demojsp.domain.accountcert.service;

import com.example.demojsp.domain.accountcert.dto.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AccountCertService {

    /**
     * 금융기관(은행/증권) 전체 목록 조회
     */
    List<BankCodeDto> getBankCodeList();

    /**
     * 대출신청 대출금 입금/출금 계좌 정보 조회 (요구사항 1: 자동완성용)
     */
    LoanAccountDto getLoanAccount(String loanNo, String memberId);

    /**
     * 1원 송금 요청 (계좌 실명조회 -> 예금주명 일치 검증 -> 1원 이체)
     */
    OneWonTransferResDto requestOneWonTransfer(OneWonTransferReqDto reqDto, HttpServletRequest request);

    /**
     * 1원 인증번호(4자리) 검증 (10분 유효, 5회 실패 제한)
     */
    OneWonVerifyResDto verifyOneWon(OneWonVerifyReqDto verifyDto, HttpServletRequest request);
}
