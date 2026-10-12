package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendResultDto {
    private boolean success;
    private String resultCode;       // 0000: 발송성공
    private String resultMessage;
    private Long smsSeq;             // TB_SMS_SEND_QUEUE 시퀀스
    private String messageId;        // 중계사 발송 메시지 ID
}
