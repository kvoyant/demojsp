package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.domain.ResumePoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 이어하기 저장소 (기본 구현). 초기 데이터는 {@code ResumeService} 가 기동 시 seed 로 적재한다.
 * 재기동하면 seed 상태로 돌아간다 (local 테스트 용도).
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "memory")
public class InMemoryResumeRepository implements ResumeRepository {

    private final Map<Long, ResumePoint> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(5000);

    @Override
    public synchronized List<ResumePoint> findByCustomer(String customerId) {
        List<ResumePoint> list = new ArrayList<>();
        for (ResumePoint p : store.values()) {
            if (p.customerId().equals(customerId)) list.add(p);
        }
        return list;
    }

    @Override
    public synchronized Optional<ResumePoint> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long nextId() {
        return sequence.incrementAndGet();
    }

    @Override
    public synchronized void save(ResumePoint point) {
        store.put(point.id(), point);
    }

    @Override
    public synchronized List<ResumePoint> findAll() {
        return new ArrayList<>(store.values());
    }
}
