package com.example.demojsp.common.niceid.gateway;

import java.util.Optional;

/**
 * NICE ID 연동부 (외부 시스템 경계). 서비스는 이 인터페이스만 알고, 실제 통신 방식은 구현체가 정한다.
 *
 * <pre>
 *  local   : MockNiceIdGateway  (common.niceid.gateway=mock, 기본) - 인증번호를 만들어 메모리에 보관하는 시뮬레이터
 *  운영    : NICE 계약 후 실연동 구현체를 만들어 common.niceid.gateway=nice 로 교체
 *            (기존 domain/auth/client/NiceApiClientImpl 의 토큰·암호화 코드를 옮겨 쓰면 된다)
 * </pre>
 * 구현체는 통신 오류 시 {@link NiceIdGatewayException} 을 던진다.
 */
public interface NiceIdGateway {

    /** 통신사 미지정 SMS 인증번호 발송 (성명 + 생년월일 + 휴대폰번호, 통신사 코드 없음) */
    SmsStarted startSms(String name, String birthDt, String phone);

    /** 인증번호 확인 → 맞으면 본인확인 결과, 틀리면 empty */
    Optional<NiceIdIdentity> verifySms(String providerTx, String code);

    /** MO(기기 점유) 인증 시작 → 고객이 휴대폰에서 moNumber 로 moCode 문자를 보내면 인증된다 */
    MoStarted startMo(String name, String birthDt);

    /** MO 문자 수신 여부 → 받았으면 본인확인 결과(휴대폰번호 포함), 아직이면 empty */
    Optional<NiceIdIdentity> checkMo(String providerTx);

    /**
     * @param providerTx NICE 거래번호
     * @param testCode   시뮬레이터만 채움 (local 화면 자동입력용). 실연동은 null
     */
    record SmsStarted(String providerTx, String testCode) {
    }

    record MoStarted(String providerTx, String moNumber, String moCode) {
    }
}
