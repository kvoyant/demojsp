package com.example.demojsp.common.identity.domain;

import java.time.Instant;
import java.util.Map;

/**
 * 진행 중인 본인인증 거래 (서버 내부 보관용, 화면에 절대 노출하지 않음).
 *
 * <p>인증 시작 시 생성되어 {@link com.example.demojsp.common.identity.store.IdentityTxStore} 에 저장되고,
 * 콜백이 들어오면 꺼내서 1회 사용 후 즉시 삭제된다. (재사용/재전송 공격 방지)
 *
 * @param txId           공통모듈 거래번호 = 인증기관에 넘기는 요청번호(requestno)
 * @param correlationKey 콜백 파라미터만으로 거래를 찾기 위한 키.
 *                       NICE 는 requestno 가 암호문 안에 들어있어서 복호화 전에는 알 수 없으므로,
 *                       평문으로 돌아오는 token_version_id 를 키로 사용한다.
 * @param method         인증수단
 * @param channel        채널
 * @param purpose        목적
 * @param sessionId      인증을 시작한 사용자 세션 ID (콜백 세션과 일치하는지 검증)
 * @param returnUrl      APP 채널 완료 후 이동 경로 (화이트리스트 검증 완료된 값)
 * @param providerState  Provider 전용 상태값 (NICE: 대칭키/IV/HMAC키 등). 공통모듈은 해석하지 않음.
 * @param createdAt      생성 시각
 * @param expiresAt      만료 시각 (기본 10분, 설정 가능)
 */
public record IdentityTx(
        String txId,
        String correlationKey,
        AuthMethod method,
        AuthChannel channel,
        AuthPurpose purpose,
        String sessionId,
        String returnUrl,
        Map<String, String> providerState,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /** 로그 출력 시 providerState(암호키)가 찍히지 않도록 toString 재정의 */
    @Override
    public String toString() {
        return "IdentityTx[txId=" + txId + ", method=" + method + ", channel=" + channel
                + ", purpose=" + purpose + ", expiresAt=" + expiresAt + "]";
    }
}
