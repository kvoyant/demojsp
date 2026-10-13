package com.example.demojsp.domain.accountcert.mapper;

import com.example.demojsp.domain.accountcert.dto.AccountCertHistDto;
import com.example.demojsp.domain.accountcert.dto.BankCodeDto;
import com.example.demojsp.domain.accountcert.dto.LoanAccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountCertMapper {

    /**
     * 금융기관(은행/증권) 전체 사용 목록 조회
     */
    List<BankCodeDto> selectBankCodeList();

    /**
     * 특정 은행코드 정보 조회
     */
    BankCodeDto selectBankCode(@Param("bankCd") String bankCd);

    /**
     * 대출신청번호 기준 등록된 대출금 입금/출금 계좌 조회 (자동완성용)
     */
    LoanAccountDto selectLoanAccountByLoanNo(@Param("loanNo") String loanNo);

    /**
     * 회원 ID 기준 최근 대출신청 계좌 조회
     */
    LoanAccountDto selectLoanAccountByMemberId(@Param("memberId") String memberId);

    /**
     * 1원 계좌인증 감사 이력 등록
     */
    int insertAccountCertHist(AccountCertHistDto histDto);

    /**
     * 인증 트랜잭션 번호 기준 이력 조회
     */
    AccountCertHistDto selectAccountCertHistByTxSeq(@Param("reqTxSeq") String reqTxSeq);

    /**
     * 인증코드 입력 오류 횟수 증가
     */
    int increaseFailCount(@Param("reqTxSeq") String reqTxSeq);

    /**
     * 인증 상태를 실패(FAILED, 5회 초과 잠금)로 변경
     */
    int updateStatusToFailed(@Param("reqTxSeq") String reqTxSeq);

    /**
     * 인증 상태를 성공(VERIFIED)으로 변경
     */
    int updateStatusToVerified(@Param("reqTxSeq") String reqTxSeq);
}
