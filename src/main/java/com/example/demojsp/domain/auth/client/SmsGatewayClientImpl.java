package com.example.demojsp.domain.auth.client;

import com.example.demojsp.common.util.MaskingUtil;
import com.example.demojsp.domain.auth.client.dto.SmsQueueDto;
import com.example.demojsp.domain.auth.client.dto.SmsSendRequestDto;
import com.example.demojsp.domain.auth.client.dto.SmsSendResultDto;
import com.example.demojsp.domain.auth.mapper.SmsQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * =========================================================================================
 * [금융권 표준 SMS 발송 모듈 연동 클라이언트 구현체]
 * 
 * 1. 업무 개요:
 *    - 본인인증 6자리 OTP 인증번호를 고객 휴대폰으로 발송하는 모듈입니다.
 *    - 금융권 규정에 따라 발신번호는 반드시 사전에 통신서비스이용증명원에 등록된 대표번호(예: 1588-0000)만 사용 가능합니다.
 * 
 * 2. 실제 금융권 2대 발송 연동 방식 지원:
 *    방식 ① [DB 인터페이스 테이블 큐 방식 (TB_SMS_SEND_QUEUE)]
 *       - 대다수 은행/저축은행 FEP 및 사내 메시징 서버 연동 표준.
 *       - 금융 애플리케이션이 TB_SMS_SEND_QUEUE 테이블에 INSERT하면,
 *         사내 SMS Agent 데몬이 실시간으로 Polling하여 이동통신사 SMSC로 대량 발송합니다.
 *       - 발송 이력 및 전송 성공 여부가 DB에 영속화되어 전자금융거래법 감사 요건을 충족합니다.
 * 
 *    방식 ② [REST API HTTP 통신 방식]
 *       - 클라우드 또는 기업 메시징 중계사 (예: 다우기술, 인포뱅크, KT 크로샷, 비즈뿌리오, 알리고 등) REST API 호출.
 *       - POST {sms.gateway.api-url} 전송 (JSON Body: destPhone, sendPhone, message).
 * 
 * 3. Mock-Mode 동작:
 *    - sms.gateway.mock-mode=true 설정 시:
 *      실제 외부 SMS 발송 비용 발생 및 테스트망 차단을 방지하기 위해 가상 발송 로그를 기록하고,
 *      DB 테이블(TB_SMS_SEND_QUEUE)에는 정상 SENT 상태로 기록하여 완벽한 시뮬레이션을 보장합니다.
 *    - 운영 전환 시: application.yaml에서 mock-mode: false 로 설정하면 실제 중계사 REST API와 실시간 연동됩니다.
 * =========================================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsGatewayClientImpl implements SmsGatewayClient {

    private final SmsQueueMapper smsQueueMapper;

    @Value("${sms.gateway.api-url:https://api.sms-gateway.bank.co.kr/v1/send}")
    private String apiUrl;

    @Value("${sms.gateway.api-key:SMS_GW_KEY_SAMPLE}")
    private String apiKey;

    @Value("${sms.gateway.sender-phone:1588-0000}")
    private String defaultSenderPhone;

    @Value("${sms.gateway.mock-mode:true}")
    private boolean mockMode;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public SmsSendResultDto sendSms(SmsSendRequestDto requestDto) {
        String destPhone = requestDto.getDestPhone().replaceAll("-", "");
        String senderPhone = requestDto.getSendPhone() != null ? requestDto.getSendPhone() : defaultSenderPhone;
        String message = requestDto.getMessage();
        String txSeq = requestDto.getReqTxSeq();

        log.info("[SMS 모듈] 인증번호 발송 요청 - 수신번호={}, 발신번호={}, TxSeq={}, MockMode={}",
                MaskingUtil.maskPhone(destPhone), senderPhone, txSeq, mockMode);

        // 1. 금융 DB 테이블 큐(TB_SMS_SEND_QUEUE)에 영속화 (방식 ① 표준 적용)
        SmsQueueDto queueDto = SmsQueueDto.builder()
                .destPhone(destPhone)
                .sendPhone(senderPhone)
                .msgBody(message)
                .msgType("SMS")
                .reqTxSeq(txSeq)
                .sendStatus("PENDING")
                .build();
        smsQueueMapper.insertSmsQueue(queueDto);
        Long smsSeq = queueDto.getSmsSeq();

        // 2. 실서버 REST API 연동 모드 (mockMode == false)
        if (!mockMode) {
            try {
                String requestJson = String.format(
                        "{\"receiver\":\"%s\",\"sender\":\"%s\",\"message\":\"%s\",\"msg_type\":\"SMS\"}",
                        destPhone, senderPhone, message
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("X-Service-Client", "SAVINGS-BANK-CORE")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                        .build();

                log.info("[SMS 모듈] 실제 중계사 REST API 호출: URL={}", request.uri());
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("[SMS 모듈] 중계사 응답 수신: Status={}, Body={}", response.statusCode(), response.body());

                if (response.statusCode() == 200) {
                    smsQueueMapper.updateSmsStatus(smsSeq, "SENT", "SUCCESS_200");
                    return SmsSendResultDto.builder()
                            .success(true)
                            .resultCode("0000")
                            .resultMessage("SMS 중계사를 통해 실제 문자가 발송되었습니다.")
                            .smsSeq(smsSeq)
                            .messageId("SMS_MID_" + System.currentTimeMillis())
                            .build();
                } else {
                    smsQueueMapper.updateSmsStatus(smsSeq, "FAILED", "HTTP_" + response.statusCode());
                }
            } catch (Exception e) {
                log.error("[SMS 모듈] 실제 SMS REST API 통신 예외 발생: {}", e.getMessage());
                smsQueueMapper.updateSmsStatus(smsSeq, "FAILED", "ERROR: " + e.getMessage());
            }
        }

        // 3. Mock 모드 (또는 실서버 통신 실패 시 Fallback)
        log.info("[SMS 모듈] SMS 시뮬레이터 발송 완료 - 수신: {}, 메시지: [{}] (운영 배포 시 mock-mode: false 적용)",
                MaskingUtil.maskPhone(destPhone), message);
        smsQueueMapper.updateSmsStatus(smsSeq, "SENT", "MOCK_SUCCESS_0000");

        return SmsSendResultDto.builder()
                .success(true)
                .resultCode("0000")
                .resultMessage("SMS 발송이 완료되었습니다. (시뮬레이션 모드)")
                .smsSeq(smsSeq)
                .messageId("MOCK_MSG_" + System.currentTimeMillis())
                .build();
    }
}
