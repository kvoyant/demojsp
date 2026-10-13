package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1원 송금 요청 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneWonTransferResDto {
    private boolean success;
    private String reqTxSeq;          // 인증 트랜잭션 일련번호
    private String bankCd;            // 은행코드
    private String bankNm;            // 은행명
    private String accountNoMasked;   // 마스킹된 계좌번호 (예: 123402-**-***890)
    private String accountHolderNm;   // 확인된 예금주명
    private String printText;         // 통장 인자문구 (예: 저축7294)
    private String testAuthCode;      // 테스트 시뮬레이션용 4자리 인증번호 (화면 편의 안내용)
    private int expiresInSeconds;     // 유효시간(초) - 600초(10분)
    private String status;            // REQUESTED
    private String message;
}
