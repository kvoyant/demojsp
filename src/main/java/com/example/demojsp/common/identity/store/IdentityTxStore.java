package com.example.demojsp.common.identity.store;

import com.example.demojsp.common.identity.domain.IdentityTx;

import java.util.Optional;

/**
 * 진행 중 인증거래 저장소.
 *
 * <p>왜 HttpSession 이 아니라 별도 저장소인가?
 * <ul>
 *   <li>NICE 콜백은 외부 도메인에서 들어오므로 브라우저 쿠키 정책(SameSite)에 따라 세션 쿠키가 빠질 수 있다.
 *       거래는 콜백 파라미터(correlationKey)만으로 찾을 수 있어야 한다.</li>
 *   <li>다중 WAS 환경에서는 Redis 구현체로 교체하면 된다. (인터페이스만 의존하므로 서비스 수정 없음)</li>
 * </ul>
 *
 * <p>구현 규칙: {@link #consume} 은 조회와 삭제를 원자적으로 수행해야 한다. (같은 콜백 2번 처리 방지)
 * <br>Redis 라면 GETDEL 명령 사용, TTL 은 tx.expiresAt 기준으로 설정.
 */
public interface IdentityTxStore {

    void save(IdentityTx tx);

    /** 조회 + 삭제 (1회용). 없으면 empty */
    Optional<IdentityTx> consume(String correlationKey);
}
