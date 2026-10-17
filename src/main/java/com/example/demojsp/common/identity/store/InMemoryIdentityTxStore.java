package com.example.demojsp.common.identity.store;

import com.example.demojsp.common.identity.domain.IdentityTx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 단일 서버용 메모리 거래 저장소 (기본 구현).
 *
 * <p><b>운영이 다중 WAS(이중화) 라면 반드시 Redis 등 공유 저장소 구현체로 교체할 것.</b>
 * 메모리 저장소는 인증 시작 서버와 콜백 수신 서버가 다르면 거래를 찾지 못한다.
 * (L4 sticky session 으로 우회 가능하지만 권장하지 않음)
 *
 * <p>교체 방법: RedisIdentityTxStore 를 만들고 {@code @Primary} 를 붙여 등록하면 서비스는 그쪽을 주입받는다.
 * (또는 이 클래스의 @Component 를 제거)
 */
@Component
public class InMemoryIdentityTxStore implements IdentityTxStore {

    private final Map<String, IdentityTx> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryIdentityTxStore(@Qualifier("identityClock") Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(IdentityTx tx) {
        store.put(tx.correlationKey(), tx);
    }

    @Override
    public Optional<IdentityTx> consume(String correlationKey) {
        if (correlationKey == null) {
            return Optional.empty();
        }
        // ConcurrentHashMap.remove 는 원자적 → 동시에 같은 콜백이 2번 와도 1번만 성공
        return Optional.ofNullable(store.remove(correlationKey));
    }

    /** 사용자가 인증창을 닫아 콜백이 오지 않은 거래 정리 (1분 주기) */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        var now = clock.instant();
        store.values().removeIf(tx -> tx.isExpired(now));
    }

    /** 테스트/모니터링용 */
    public int size() {
        return store.size();
    }
}
