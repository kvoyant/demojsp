package com.example.demojsp.domain.accountcert.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 펌뱅킹 1원 송금 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmBankingTransferResultDto {
    private boolean success;
    private String resCode;           // "0000": 정상 송금
    private String resMessage;
    private String bankTranId;        // 은행 거래고유번호 (펌뱅킹 전문일련번호)
    private String tranDate;          // 거래일자 (YYYYMMDD)
    private String tranTime;          // 거래시간 (HHmmss)
    private Long tranAmount;          // 거래금액 (1원)
    private String printText;         // 통장 인자문구
}
