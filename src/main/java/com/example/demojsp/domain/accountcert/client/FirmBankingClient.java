package com.example.demojsp.domain.accountcert.client;

import com.example.demojsp.domain.accountcert.client.dto.AccountInquiryResultDto;
import com.example.demojsp.domain.accountcert.client.dto.FirmBankingTransferResultDto;

/**
 * =========================================================================================
 * [금융권 펌뱅킹 / 오픈뱅킹 타행 연동 클라이언트 인터페이스]
 * 1. 타행 계좌 실명조회 (예금주명 및 계좌 정상 상태 확인)
 * 2. 타행 1원 이체 송금 (입금통장 적요에 4자리 인증번호 인자)
 * =========================================================================================
 */
public interface FirmBankingClient {

    /**
     * 타행 계좌 실명 및 상태 조회
     * @param bankCd 은행코드 (3자리)
     * @param accountNo 계좌번호
     * @return 계좌 실명조회 결과
     */
    AccountInquiryResultDto inquireAccountHolder(String bankCd, String accountNo);

    /**
     * 타행 계좌로 1원 송금 (인증 적요 표기)
     * @param bankCd 은행코드 (3자리)
     * @param accountNo 계좌번호
     * @param printText 통장 인자문구 (예: "저축7294")
     * @param reqTxSeq 인증 트랜잭션 번호
     * @return 1원 이체 결과
     */
    FirmBankingTransferResultDto transferOneWon(String bankCd, String accountNo, String printText, String reqTxSeq);
}
