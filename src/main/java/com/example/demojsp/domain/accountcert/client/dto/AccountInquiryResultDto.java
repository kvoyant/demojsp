package com.example.demojsp.domain.accountcert.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 펌뱅킹 계좌 실명조회 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInquiryResultDto {
    private boolean success;
    private String resCode;           // "0000": 정상
    private String resMessage;
    private String bankCd;
    private String bankNm;
    private String accountNo;
    private String accountHolderNm;   // 조회된 실제 예금주명
    private String accountStatus;     // NORMAL: 정상, SUSPENDED: 거래정지, CLOSED: 해지
}
