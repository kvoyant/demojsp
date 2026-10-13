package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1원 인증번호 검증 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneWonVerifyReqDto {
    private String reqTxSeq;    // 인증 트랜잭션 일련번호
    private String authCode;    // 사용자가 입력한 4자리 숫자 (예: 7294)
}
