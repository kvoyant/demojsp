package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대출신청 대출금 입금/출금 계좌 DTO (요구사항 1: 자동완성 처리 데이터)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccountDto {
    private String loanNo;            // 대출신청번호 (예: LN202610130001)
    private String memberId;          // 고객ID
    private String custNm;            // 고객명 (신청인)
    private String productNm;         // 대출상품명
    private String bankCd;            // 등록된 은행코드 (예: 004)
    private String bankNm;            // 등록된 은행명 (예: KB국민은행)
    private String accountNo;         // 등록된 계좌번호
    private String accountHolderNm;   // 예금주명
    private String accountType;       // 계좌용도 (DISBURSEMENT_REPAYMENT)
    private LocalDateTime regDt;      // 등록일시
}
