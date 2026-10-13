package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1원 송금 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneWonTransferReqDto {
    private String purpose;       // LOAN_APPLY (대출신청연계), COMMON (타업무공통)
    private String loanNo;        // 대출신청번호 (대출연계 시)
    private String memberId;      // 고객ID (세션 또는 전달값)
    private String custNm;        // 본인확인 실명 (예금주명과 일치해야 함)
    private String bankCd;        // 입금 은행코드
    private String accountNo;     // 입금 계좌번호 (숫자 및 하이픈)
    private String isModified;    // 자동완성 후 수정 여부 (Y/N)
}
