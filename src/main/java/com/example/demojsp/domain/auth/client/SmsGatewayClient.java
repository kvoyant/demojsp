package com.example.demojsp.domain.auth.client;

import com.example.demojsp.domain.auth.client.dto.SmsSendRequestDto;
import com.example.demojsp.domain.auth.client.dto.SmsSendResultDto;

public interface SmsGatewayClient {

    /**
     * 금융 SMS 발송 모듈 연동
     * - 실제 SMS 중계사(REST API) 연동 및 금융권 메시지 큐(TB_SMS_SEND_QUEUE) 적재 동시 지원
     */
    SmsSendResultDto sendSms(SmsSendRequestDto requestDto);
}
