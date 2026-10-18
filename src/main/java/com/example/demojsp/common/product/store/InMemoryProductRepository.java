package com.example.demojsp.common.product.store;

import com.example.demojsp.common.product.domain.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** 메모리 상품 저장소 (seed 는 ProductService 가 기동 시 적재, 재기동 시 초기화) */
@org.springframework.stereotype.Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(300);

    @Override
    public synchronized List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized Optional<Product> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long nextId() {
        return sequence.incrementAndGet();
    }

    @Override
    public synchronized void save(Product product) {
        store.put(product.id(), product);
    }
}
