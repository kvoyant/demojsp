package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsQueueDto {
    private Long smsSeq;
    private String destPhone;
    private String sendPhone;
    private String msgBody;
    private String msgType;
    private String reqTxSeq;
    private String sendStatus;     // PENDING, SENT, FAILED
    private String extResultCode;
    private LocalDateTime regDt;
    private LocalDateTime sentDt;
}
