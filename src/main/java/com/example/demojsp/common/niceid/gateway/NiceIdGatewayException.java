package com.example.demojsp.common.niceid.gateway;

/** NICE 연동 오류 (통신 실패, 거래번호 없음 등) - 서비스가 NID-201 로 바꿔 안내한다 */
public class NiceIdGatewayException extends RuntimeException {

    public NiceIdGatewayException(String message) {
        super(message);
    }
}
