package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendRequestDto {
    private String destPhone;       // 수신번호 (고객 휴대폰번호)
    private String sendPhone;       // 발신번호 (금융사 대표번호)
    private String message;         // 메시지 내용
    private String reqTxSeq;        // 본인인증 트랜잭션 번호
}
