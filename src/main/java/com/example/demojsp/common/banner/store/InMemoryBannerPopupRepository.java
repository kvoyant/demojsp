package com.example.demojsp.common.banner.store;

import com.example.demojsp.common.banner.domain.BannerPopup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 배너팝업 저장소 (기본 구현). 초기 데이터는 {@code BannerPopupService} 가 기동 시 seed 로 적재한다.
 *
 * <p>ADMIN API 로 등록한 배너는 메모리에만 있으므로 재기동하면 seed 상태로 돌아간다. (local 테스트 용도)
 */
@org.springframework.stereotype.Repository
public class InMemoryBannerPopupRepository implements BannerPopupRepository {

    private final Map<Long, BannerPopup> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public synchronized List<BannerPopup> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized Optional<BannerPopup> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long nextId() {
        return sequence.incrementAndGet();
    }

    @Override
    public synchronized void save(BannerPopup banner) {
        store.put(banner.id(), banner);
    }

    @Override
    public synchronized boolean delete(long id) {
        return store.remove(id) != null;
    }
}
