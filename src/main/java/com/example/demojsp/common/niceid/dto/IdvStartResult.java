package com.example.demojsp.common.niceid.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 본인인증 시작 응답 - 화면은 reqNo 를 들고 다음 단계(인증번호 확인 / MO 상태 확인)를 호출한다 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdvStartResult {

    private final String reqNo;
    private final String method;          // SMS / MO
    private final LocalDateTime expireAt; // 인증번호 / MO 대기 만료
    private final int validSec;           // 남은 시간 표시용 (초)
    private final int maxTry;
    private final String phoneMask;       // SMS: 010-****-5678 로 보냈습니다
    private final String moNumber;        // MO: 이 번호로 문자를 보내세요
    private final String moCode;          // MO: 문자 내용
    private final String smsUri;          // MO: APP 에서 문자앱 바로 열기 (sms:번호?body=내용)
    private final String testCode;        // local 전용 (common.niceid.expose-test-code=true) - 운영은 항상 null
}
