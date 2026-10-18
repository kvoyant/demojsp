package com.example.demojsp.common.product.store;

import com.example.demojsp.common.product.domain.Product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 원장 저장소 (ADMIN 수신상품목록 / 여신상품목록).
 * 운영은 기존 상품 테이블 MyBatis 구현체로 교체 ({@code @Primary}) - 홈 표시여부·순서 컬럼 2개 추가 필요 (공통#48).
 */
public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(long id);

    long nextId();

    void save(Product product);
}
