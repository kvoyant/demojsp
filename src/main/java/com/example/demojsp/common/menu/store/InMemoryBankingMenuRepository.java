package com.example.demojsp.common.menu.store;

import com.example.demojsp.common.menu.domain.BankingMenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** 메모리 뱅킹메뉴 저장소 (seed 는 BankingMenuService 가 기동 시 적재, 재기동 시 초기화) */
@org.springframework.stereotype.Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "common.store.type", havingValue = "memory")
public class InMemoryBankingMenuRepository implements BankingMenuRepository {

    private final Map<Long, BankingMenu> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100);

    @Override
    public synchronized List<BankingMenu> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized Optional<BankingMenu> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long nextId() {
        return sequence.incrementAndGet();
    }

    @Override
    public synchronized void save(BankingMenu menu) {
        store.put(menu.id(), menu);
    }
}
